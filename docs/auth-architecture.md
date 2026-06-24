# 认证授权架构文档

> 审计日期：2026-05-24 | 最后更新：2026-06-21（Phase 6：sys_user ↔ oauth_user 字段归属/同步表（§6.2）；`expire_date` 镜像 sys_user 且清除链路接通（§5.5、§GAP-7）；`status` 双层语义——登录闸门走 `enabled`、UPMS 访问走 `sys_user.status`（§5.5、§5.6、§4.2）；`SysUserVO` 内嵌 `oauthVO` 聚合认证侧信息（§5.5）；GAP-7 引入 JsonNullable 三态更新基础设施（§11、§5.5）；Phase 2：MFA TOTP 双因素认证（§5.10、§3.2、§6.1、§8、§10）；BusinessException / InterfaceBusinessException HTTP 状态从 500 改为 422（§9-F）；新增集群安全已知缺口 GAP-9/GAP-10（§11））
> 审计范围：snack-oauth、snack-upms、oauth2 starter、snack-project-web

---

## 目录

1. [三个核心角色](#1-三个核心角色)
2. [请求流转](#2-请求流转)
3. [Token 设计](#3-token-设计)
4. [权限模型](#4-权限模型)
5. [关键场景](#5-关键场景)
6. [数据库关键表](#6-数据库关键表)
7. [部署模式](#7-部署模式)
8. [关键配置速查](#8-关键配置速查)
9. [HTTP 响应速查](#9-http-响应速查)
10. [关键源码位置](#10-关键源码位置)
11. [已知缺口](#11-已知缺口)

---

## 1. 三个核心角色

项目采用 **SAS + BFF** 模式，三个角色各司其职：

```
  ┌─────────────┐  Session Cookie  ┌──────────────────────────────────────────────┐
  │   前端 SPA   │◄────────────────►│           snack-upms-server                  │
  └─────────────┘                  │                                              │
                                   │  ┌──────────────────────────────────────┐   │
                                   │  │  ② OAuth2 Client / BFF（Order 2）    │   │
                                   │  │  SessionStateCheckFilter              │   │
                                   │  │  AT/RT 存入 HttpSession               │   │
                                   │  └──────────────────────────────────────┘   │
                                   │  ┌──────────────────────────────────────┐   │
                                   │  │  ③ Resource Server（Order 3）        │   │
                                   │  │  /api/** JWT 验证 + RBAC 授权         │   │
                                   │  └──────────────────────────────────────┘   │
                                   └──────────────────────────────────────────────┘
                                                    │ OAuth2 协议交互
                                   ┌────────────────▼─────────────────────────────┐
                                   │  ① SAS — snack-oauth-biz（Order 1）          │
                                   │  颁发 JWT、验证用户凭证、持久化 Token          │
                                   │  OAuth2TokenCustomizerImpl（Token 定制）      │
                                   └──────────────────────────────────────────────┘
                                                    │ JDBC（oauth_*）
                                   ┌────────────────▼─────────────────────────────┐
                                   │  Database                                    │
                                   │  oauth_user / oauth_authorization ← SAS     │
                                   │  sys_user / sys_role / sys_login_log ← upms │
                                   └──────────────────────────────────────────────┘
```

---

### 1.1 SAS（Spring Authorization Server）

**部署**：`snack-oauth-biz`。可内嵌于 `snack-upms-server`（合并部署），也可作为独立 `snack-oauth-server` 进程（分离部署）。

**安全链**：Order 1，`AuthorizationServerSecurityFilterChain`，匹配 `/oauth2/**`、`/.well-known/**`。

**职责**：

- 执行 OAuth2 授权码流程，验证用户凭证（委托 `LockCheckingUserDetailsService` → `UserDetailsServiceImpl`）
- 颁发 JWT AccessToken / RefreshToken / ID Token，通过 `JdbcOAuth2AuthorizationService` 持久化
- 通过 `OAuth2TokenCustomizerImpl` 注入业务语义：
  - `authorization_code` grant：检测 authorities 含 `SCOPE_pre_auth_reset`（由 `PasswordChangeRestriction.appliesTo()` → `UserDetailsServiceImpl` 注入）→ 权限降级，TTL 压缩至 5 分钟
  - `refresh_token` grant：重新从 DB 加载用户状态，状态异常（密码过期/禁用/锁定）则拒绝续发
- 通过 `PreAuthRestrictionFilter`（注册于 `AuthorizationServerSecurityFilterChain`）拦截 `/oauth2/authorize`：已认证但持有受限权限的用户保存请求后重定向至前端处理页，改密完成后原路返回
- 支持 Token 撤销（`/oauth2/revoke`）

**关键端点**：

| 端点                      | 作用                                    |
|-------------------------|---------------------------------------|
| `GET /oauth2/authorize`                | 授权码流程入口，重定向至登录页                             |
| `POST /login`                          | 表单登录（前端 SPA 经 `/auth-api/login` 代理访问）       |
| `POST /oauth2/token`                   | 授权码 / refresh_token 换 AT                      |
| `GET /oauth2/jwks`                     | RSA 公钥，Resource Server 用于验签                   |
| `POST /oauth2/revoke`                  | 撤销 AT 或 RT                                    |
| `GET /userinfo`                        | OIDC 用户信息                                     |
| `POST /oauth2/account/change-password` | 受限用户就地改密 + session 升级，返回 `{"redirectUrl":"..."}` |

---

### 1.2 OAuth2 Client / BFF

**部署**：`snack-oauth2-client-spring-boot-starter`（自动配置），挂载于 `snack-upms-server`。

**安全链**：Order 2，`OAuth2ClientSecurityFilterChain`，匹配所有请求。

**职责**：

- 执行授权码流程，与 SAS 交换 AT/RT，存入服务端 `HttpSession`
- **前端只持有 Session Cookie，从不直接持有 JWT**
- 通过 `SessionStateCheckFilter`（挂载于 `SecurityContextHolderFilter` 之后）对每次已认证请求触发 AT 懒刷新；`SessionRefreshLock`（`CacheSessionRefreshLock`）防止同一 Session 并发重复刷新（Caffeine/Redis 自适应）：
  - AT 未过期 → 取缓存，无网络开销
  - AT 过期 → 持锁（超时 3 秒降级放行）后用 RT 向 SAS 刷新，SAS 侧重新验证用户状态
  - SAS 拒绝续发（`OAuth2AuthorizationException`）→ 强制使 Session 失效，返回 HTTP 401
- 通过 `SessionRegistry` 维护活跃 Session 表；`maxSessions > 0` 时启用并发 Session 限制：新设备登录后旧 Session 被标记过期，`JsonExpiredSessionStrategy` 返回 HTTP 401 JSON（含 i18n 提示）
- 处理登出：撤销 Token → 销毁 Session → 返回 `{"redirectUrl":"..."}`

**Customizer 链**（`OAuth2ClientSecurityCustomizer` SPI，按 `getOrder()` 注入）：

| Customizer                            | Order  | 作用                                                                                                              |
|---------------------------------------|--------|-----------------------------------------------------------------------------------------------------------------|
| `UpmsSecurityConfiguration`（upms-biz） | -100   | 提供 `UpmsGrantedAuthoritiesMapper`（OAuth2 登录回调时加载 ROLE_* 和资源权限）；放行 `PUT /api/upms/users/password`、`GET /api/upms/users/info`、`GET /api/upms/users/resources`（authenticated） |
| `OAuth2SecurityCustomizer`（oauth-biz） | 0      | 注册路径规则（见下）；收集所有 `AuthorizationManager` Bean 组成 `securityPolicies` 链                                             |
| `OAuthFormLoginCustomizer`（oauth-biz） | LOWEST | 配置 formLogin JSON handlers；`exceptionHandling`：`/api/**` → 401 JSON，其他路径 → 302；`oauth2Login.successHandler` 包装 `RememberMeAwareSuccessHandler` |

`OAuth2SecurityCustomizer` 注册的路径规则（首匹配，顺序固定）：

```
/api/oauth/user/**      → hasAuthority("SCOPE_client")  仅服务客户端可访问
/api/**                  → securityPolicies 链
    ├─ [Order   1] PreAuthAuthorizationManager（oauth-biz）
    │               含任意 pre-auth restriction 权限（如 SCOPE_pre_auth_reset）→ 403
    ├─ [Order 100] UpmsDynamicAuthorizationManager（upms-biz）
    │               sys_user.status 禁用 → 403（含管理员）；ROLE_ADMIN → 放行；URL 不在 sys_resource → 403；否则校验权限串
    └─ fallback    AuthenticatedAuthorizationManager → 已认证则放行
```

---

### 1.3 Resource Server

**部署**：`snack-oauth2-resource-server-spring-boot-starter`（自动配置），挂载于 `snack-upms-server`。分离部署时 `snack-oauth-server` 也用此 Starter 保护自身管理端点。

**安全链**：Order 3，`ResourceServerSecurityFilterChain`，匹配 `/api/**`。

**职责**：

- 验证 Bearer JWT 的签名、有效期、`aud` 字段
- 提取 JWT `authorities` claim，构建 `Authentication`
- 对 `/api/**` 执行 `securityPolicies` 授权链（见 §4.2）

**BFF 模式下的实际调用路径**：

前端携带 Session Cookie → Order 2 链（OAuth2 Client）处理认证和 AT 管理 → 业务请求本身**不带 Bearer 头**，Controller 直接处理。Order 3 链（Resource Server）主要生效于内部服务调用：UPMS 使用 `snack-upms-service` client_credentials AT 携带 Bearer 头访问 oauth-server 管理端点（`/api/oauth/user/**`）。

---

## 2. 请求流转

### 2.1 用户登录（前端 SPA → BFF → SAS）

前端 SPA 自带登录页（`/account/login`），不依赖 SAS 的 Server-Side 渲染登录页。

```
前端 SPA (/account/login)     BFF (Order 2)                   SAS (Order 1)
  │                                │                                │
  │ 访问受保护路由                  │                                │
  │──────────────────────────────>│                                │
  │                                │ onPageChange: 无 currentUser  │
  │<── history.replace('/account/login?redirect=...')                 │
  │                                │                                │
  │ [用户填写账密，点击登录]          │                                │
  │ POST /auth-api/login           │                                │
  │ (代理到 BFF 的 /login)         │                                │
  │──────────────────────────────>│                                │
  │                                │ ① LockCheckingUserDetailsService
  │                                │ ② UserDetailsServiceImpl（查 DB）
  │                                │ ③ BCrypt 密码校验
  │                                │ ④ OAuth2TokenCustomizerImpl
  │                                │    检测 initialPassword / 密码过期
  │                                │    → 正常：ROLE_USER
  │                                │    → 异常：scope=pre_auth_reset, TTL=5min
  │<── 200 {"code":null,"data":{"redirectUrl":"<savedUrl>"}}       │
  │                                │                                │
  │ [若勾选记住我] document.cookie = 'x-remember-me=1; max-age=300; ...'
  │ window.location.href = redirectUrl                             │
  │──────────────────────────────────────────────────────────────>│
  │<── 302 → /login/oauth2/code?code=xxx                          │
  │ GET /login/oauth2/code?code=xxx                               │
  │──────────────────────────────>│                                │
  │                                │ POST /oauth2/token (code 换 token)
  │                                │──────────────────────────────>│
  │                                │<── AT(5min) + RT(1h) + ID Token│
  │                                │ GET /userinfo                  │
  │                                │──────────────────────────────>│
  │                                │<── OIDC 用户信息               │
  │                                │ UpmsGrantedAuthoritiesMapper   │
  │                                │ 追加 ROLE_* 和资源权限          │
  │                                │ 建立 HttpSession（存入 AT/RT）  │
  │                                │ RememberMeAwareSuccessHandler  │
  │                                │ 读取 x-remember-me cookie      │
  │                                │   → session.setMaxInactiveInterval(30天)
  │                                │   → 写持久化 session cookie     │
  │                                │   → 删除意图 cookie（不可重放） │
  │<─────────────────────────────── 302 → defaultSuccessUrl        │
  │                                │                                │
  │ app.tsx getInitialState()      │                                │
  │ GET /api/upms/users/info       │                                │
  │──────────────────────────────>│──────────────────────────────>│
  │<── SysUserVO（含 initialPassword / expired）                   │
  │ 若 initialPassword=1 或 expired=1 → /account/change-password  │
  │ 否则 → 正常进入应用             │                                │
```

> **代理配置**（dev 环境 `config/proxy.ts`）：
> - `POST /auth-api/login` → `OAUTH_TARGET/login`（表单登录端点）
> - `POST /auth-api/logout` → `localhost:8080/logout`
> - `/oauth2/authorization/` → BFF 授权码启动端点
> - `/api/**` → `localhost:8080`

---

### 2.2 正常 API 访问（AT 有效）

```
前端 SPA            BFF (Order 2)       Resource Server (Order 3)    Controller
  │                        │                              │                        │
  │ GET /api/upms/users    │                              │                        │
  │ (Session Cookie)       │                              │                        │
  │───────────────────────>│                              │                        │
  │                        │ SessionStateCheckFilter      │                        │
  │                        │ authorize() → AT 未过期      │                        │
  │                        │ 取缓存，无网络开销            │                        │
  │                        │ 放行（无 Bearer 头，BFF 模式）│                        │
  │                        │──────────────────────────────────────────────────────>│
  │                        │              PasswordRestrictionAuthorizationManager  │
  │                        │              无 SCOPE_pre_auth_reset → 通过           │
  │                        │              UpmsDynamicAuthorizationManager          │
  │                        │              RBAC 校验 → 通过                         │
  │<─────────────────────────────────────────────────────── 200 业务数据 ──────────│
```

---

### 2.3 AT 到期自动刷新（OAuth2 Client ↔ SAS）

```
OAuth2 Client (SessionStateCheckFilter)              SAS (OAuth2TokenCustomizerImpl)
  │                                                          │
  │ authorize() → AT 已过期，触发刷新                        │
  │ POST /oauth2/token?grant_type=refresh_token              │
  │─────────────────────────────────────────────────────────>│
  │                                                          │ refresh_token grant 分支：
  │                                                          │ userDetailsService.loadUserByUsername()
  │                                                          │ 重新从 DB 加载用户状态
  │                                                          │
  │           [用户状态正常]                                  │
  │<─────────────────── 新 AT(5min) ─────────────────────────│
  │ 放行请求                                                  │
  │                                                          │
  │           [用户状态异常：密码过期 / 禁用 / 锁定]           │
  │<─────────────────── 400 access_denied ───────────────────│
  │ OAuth2AuthorizationException                             │
  │ → session.invalidate()                                   │
  │ → HTTP 401 JSON {"redirectUrl":"/oauth2/authorization/snack-upms-server"}
```

> `SessionStateCheckFilter` 返回格式：`{"data":{"loginUrl":"/oauth2/authorization/snack-upms-server"}}`，与 `BizAuthenticationEntryPoint` 的标准格式保持一致，前端 `requestErrorConfig.ts` 可正确识别并跳转（GAP-8 已修复）。

---

### 2.4 未认证 / Session 超时

```
前端 SPA            BFF (Order 2)
  │                        │
  │ GET /api/upms/users    │
  │ (无 Cookie 或 Session 超时)
  │───────────────────────>│
  │                        │ SessionStateCheckFilter:
  │                        │ auth 非 OAuth2AuthenticationToken → 放行
  │                        │ ExceptionTranslationFilter:
  │                        │ 无 Authentication → AuthenticationException
  │                        │   /api/**      → BizAuthenticationEntryPoint
  │                        │   其他路径     → LoginUrlAuthenticationEntryPoint
  │<───────────────────────│ /api/**：HTTP 401 JSON（loginUrl）
  │                        │ 其他路径：HTTP 302 → OAuth2 授权端点
  │ window.location.href = loginUrl → 重新走 §2.1 登录流程
```

---

### 2.5 UPMS 内部服务调用 oauth-server 管理端点

```
SysUserServiceImpl              OAuth2UserClient                oauth-server /api/oauth/user/**
  │                                    │                                    │
  │ create(dto)                        │                                    │
  │───────────────────────────────────>│                                    │
  │                                    │ OAuth2ClientHttpRequestInterceptor │
  │                                    │ 固定使用 snack-upms-service registration
  │                                    │ client_credentials → AT(SCOPE_client)│
  │                                    │ POST /api/oauth/user              │
  │                                    │ Authorization: Bearer <AT>         │
  │                                    │───────────────────────────────────>│
  │                                    │        Resource Server (Order 3)   │
  │                                    │        JWT 验证 + hasAuthority("SCOPE_client")
  │                                    │        OAuthUserAdminController    │
  │<───────────────────────────────────│<── 201 Created ────────────────────│
```

---

## 3. Token 设计

### 3.1 Token 类型与 TTL

| Token 类型              | TTL      | 说明                                   |
|-----------------------|----------|--------------------------------------|
| Authorization Code    | 5 分钟     | 一次性，换 Token 后失效                      |
| Access Token          | **5 分钟** | BFF 懒刷新，用户无感                         |
| Refresh Token         | **8 小时** | `reuseRefreshTokens=false`，每次刷新生成新 RT |
| 受限 AT（pre_auth_reset） | 5 分钟     | `OAuth2TokenCustomizerImpl` 强制覆盖 TTL |

### 3.2 JWT Claim 结构

**正常用户**：
```json
{
  "sub": "username",
  "aud": ["snack-upms-server"],
  "scope": ["openid", "profile"],
  "authorities": ["ROLE_USER"],
  "iat": 1716350000,
  "exp": 1716350300
}
```

**受限用户 — 改密**（初始密码 / 密码过期）：
```json
{
  "sub": "username",
  "scope": ["openid", "pre_auth_reset"],
  "authorities": [],
  "exp": "<签发后5分钟>"
}
```

**受限用户 — MFA 待验证**（`mfa_enabled=1`，已完成账密认证，尚未通过二次验证）：
```json
{
  "sub": "username",
  "scope": ["openid", "pre_auth_mfa"],
  "authorities": [],
  "exp": "<签发后5分钟>"
}
```

> `openid` scope 保留是为了让 ID Token 符合 OIDC 规范（`/connect/logout` 的 `id_token_hint` 校验要求含 `openid` scope）。`SCOPE_pre_auth_reset` authority 由 `OidcScopeGrantedAuthoritiesMapper` 从 scope 中映射，`PasswordRestrictionAuthorizationManager` 仍能正确拦截。

### 3.3 Token 定制逻辑（`OAuth2TokenCustomizerImpl`）

```
customize(JwtEncodingContext context)
    │
    ├─ tokenType 非 ACCESS_TOKEN 且非 ID_TOKEN → 跳过
    │
    ├─ grant_type = refresh_token
    │    └─ validateUserStateForRefresh(username)
    │         userDetailsService.loadUserByUsername()  ← 重新查 DB
    │         getAuthorities() → isCredentialsExpired() ← 时间维度过期检测
    │         !enabled / !accountNonLocked / !accountNonExpired
    │         或 authorities 含 SCOPE_pre_auth_reset（含时间过期）
    │         → throw OAuth2AuthenticationException(access_denied)
    │
    └─ grant_type = authorization_code
         principal.authorities 含 SCOPE_pre_auth_reset？
           Yes → scope={openid, pre_auth_reset}, authorities=[], TTL=5min
                 （保留 openid 使 ID Token 符合 OIDC 规范，支持 /connect/logout）
           No  → SAS 默认处理（不修改）
```

### 3.4 Audience 校验

Resource Server 校验 `aud` 必须含 `spring.application.name`（`snack-upms-server`），可配置关闭：
```yaml
snack.oauth2.resource-server.validate-audience: false
```

### 3.5 签名算法

RSA，支持多密钥轮转。所有配置的 key 均可验证，唯一 `active: true` 的 key 用于签发：
```yaml
snack.security.policy.rsa.keys:
  - key-id: snack-jwt-key-v1
    private-key: classpath:keys/jwt-private.pem
    public-key: classpath:keys/jwt-public.pem
    active: true
```

---

## 4. 权限模型

### 4.1 两层权限模型

```
┌─── 粗粒度：AT / JWT 层 ─────────────────────────────────────────┐
│  ROLE_USER             = 已认证普通用户                           │
│  SCOPE_pre_auth_reset  = 受限用户（需改密）                       │
│  变更生效窗口：最长 5 分钟（AT TTL）                              │
└─────────────────────────────────────────────────────────────────┘

┌─── 细粒度：UPMS 资源层 ──────────────────────────────────────────┐
│  通过 /api/upms/users/resources 动态查询                         │
│  @Cacheable(upms:user, key="resources:"+username)               │
│  权限变更 → CacheEvict → 立即生效                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 4.2 `securityPolicies` 授权规则链

```
/api/oauth/user/**      → hasAuthority("SCOPE_client")
    仅服务客户端（client_credentials，scope 自动包含 client）可访问
    普通用户 AT scope 仅含 openid/profile，SAS 不会颁发 SCOPE_client，永远无法匹配

/api/**（其余） → securityPolicies 链（OrderedStream 注入）
    ├─ [Order 1]   PreAuthAuthorizationManager
    │               含任意 pre-auth restriction 权限 → AuthorizationDecision(false) → 403
    ├─ [Order 100] UpmsDynamicAuthorizationManager（snack-upms-biz 同进程时注册）
    │               sys_user.status 禁用 → 403（早于 ROLE_ADMIN，即时生效）
    │               ROLE_ADMIN → 直接放行
    │               URL 不在 sys_resource → 403（deny-by-default）
    │               URL 在 sys_resource → 校验用户权限串
    └─ fallback    AuthenticatedAuthorizationManager
```

### 4.3 UPMS 权限加载时机

OAuth2 登录回调时（`/login/oauth2/code?code=...`），`UpmsGrantedAuthoritiesMapper.mapAuthorities()` 被调用：

```
OidcUserAuthority → username
    → sys_user_role → sys_role → sys_role_resource → sys_resource
    → 追加 ROLE_* 和资源权限串到 Authentication.authorities
```

后续 `UpmsDynamicAuthorizationManager` 直接读取 `Authentication.authorities`，无额外 DB 查询。

权限变更生效：管理员修改角色 → `CacheEvict(upms:user, allEntries=true)` → 用户下次请求资源列表时重新加载（UPMS 资源层立即生效，AT 层粗粒度权限最长 5 分钟延迟）。

---

## 5. 关键场景

### 5.1 新用户创建

```
管理员 POST /api/upms/users
    │
    ├─ SysUserServiceImpl
    │   ① 检查 username 唯一
    │   ② validateRoleStatus（检查角色 enabled 状态）
    │   ③ OAuth2UserClient → POST /api/oauth/user（snack-upms-service AT）
    │       ④ 创建 oauth_user：
    │           password=encode(Snack@123), enabled=1, locked=0
    │           expired=0, initialPassword=1, lastPasswordResetTime=now()
    │   ⑤ 保存 SysUser / SysUserRole / SysUserOrg
    │   ⑥ CacheEvict(upms:user)
```

oauth_user 与 sys_user 双写，OAuth2UserClient 调用失败时通过 `transactionTemplate` 回滚 UPMS 侧。

**闭环状态**：✅

---

### 5.2 初始密码强制改密 / 密码过期（登录前）

两种情形触发路径相同，均通过 `PasswordChangeRestriction.appliesTo()` 检测：

| 触发条件      | 字段                                                                               |
|-----------|----------------------------------------------------------------------------------|
| 初始密码      | `oauth_user.initialPassword = 1`                                                 |
| 密码过期（登录前） | `now > lastPasswordResetTime + 90天`（`SecurityProperties.isCredentialsExpired()`） |

```
① SAS 颁发受限 AT：
   PasswordChangeRestriction.appliesTo(user) = true
   UserDetailsServiceImpl → authorities=[SCOPE_pre_auth_reset]
   OAuth2TokenCustomizerImpl → scope={openid, pre_auth_reset}, authorities=[], TTL=5min

② 用户表单登录成功 → JsonAuthenticationSuccessHandler
   返回 {"redirectUrl": "/oauth2/authorize?..."}（SAS 在重定向至登录页前已保存此请求）

③ 前端 window.location.href = "/oauth2/authorize?..."
   [PreAuthRestrictionFilter] 用户已认证且含 SCOPE_pre_auth_reset：
   → requestCache.saveRequest()  ← 保存 /oauth2/authorize?... 至 HttpSession
   → response.sendRedirect(frontendBaseUrl + "/account/change-password")
   （frontendBaseUrl 由 SecurityProperties.getFrontendBaseUrl() 自动从 loginPage 提取）

④ 用户提交新密码 → POST /oauth2/account/change-password（SAS）
   @PreAuthorize("hasAuthority('SCOPE_pre_auth_reset')")
   OAuthUserController.changePassword()：
   ① oAuthUserService.update()：更新密码、清除 initialPassword 标记
   ② 重新加载 UserDetails，构建升级后的 Authentication：
      authorities = [ROLE_USER, FactorGrantedAuthority(PASSWORD)]
      （Spring Security 7.x JwtGenerator.getAuthenticationTime() 需要 FactorGrantedAuthority
        填充 ID Token 的 auth_time claim；缺失时抛 IllegalArgumentException → 500）
   ③ 持久化新 SecurityContext 至 HttpSession
   ④ requestCache.getRequest() 直接取回 PreAuthRestrictionFilter 保存的 /oauth2/authorize?...
   返回：{"redirectUrl": "/oauth2/authorize?..."}

⑤ 前端 window.location.href = "/oauth2/authorize?..."
   PreAuthRestrictionFilter：session 已升级，无 pre-auth restriction 权限 → 跳过
   SAS 正常颁发授权码 → /login/oauth2/code?code=xxx
   BFF：code 换 AT/RT → UpmsGrantedAuthoritiesMapper → RememberMeAwareSuccessHandler
   → SimpleUrlAuthenticationSuccessHandler → defaultSuccessUrl（http://localhost:8000/）
   → 进入系统
```

**关键设计**：
- `credentialsExpired=false` 是有意为之——若设为 `true`，框架在认证阶段抛异常，用户无法获得任何 Token，也就无法调用改密接口。权限降级方案让用户持有受限 Token，由 `PreAuthRestrictionFilter` 在 `/oauth2/authorize` 层面统一拦截重定向，是正确的设计选择。
- 改密后 **session 就地升级**，不再需要 logout + OIDC logout + re-login。`PreAuthRestrictionFilter` 在改密前已将 `/oauth2/authorize` 保存至 `HttpSessionRequestCache`，改密接口直接取回，无需 `authorizationUri` 兜底。前端跟随 redirectUrl 一次跳转即可完成完整 token 颁发流程，用户无感知。
- `frontendBaseUrl` 由 `SecurityProperties.getFrontendBaseUrl()` 自动从 `loginPage` 提取（如 `http://localhost:8000/user/login` → `http://localhost:8000`），分离/合并部署均可自适应，无需显式配置。
- `preAuthPages` Map 由 `snack.security.policy.pre-auth-pages` 配置，各 `PreAuthRestriction` 实现通过 scope key 查询自身的前端页面路径，扩展新限制类型时增加对应 Bean 和配置项即可。

**闭环状态**：✅

---

### 5.3 密码过期（会话中过期）

用户已登录且 AT scope 正常，密码在会话期间到达 90 天过期线。

```
AT 未过期（最长 5min 窗口）：
    SessionStateCheckFilter → authorize() → 取缓存 AT → 放行（无感知）

AT 到期，下次请求触发刷新：
    ① authorize() → POST /oauth2/token?grant_type=refresh_token（→ SAS）
    ② OAuth2TokenCustomizerImpl.validateUserStateForRefresh()
         userDetailsService.loadUserByUsername() ← 重新查 DB
         getAuthorities() → isCredentialsExpired() = true
         → throw OAuth2AuthenticationException(access_denied)
    ③ SAS 返回 400 access_denied
    ④ SessionStateCheckFilter 捕获 OAuth2AuthorizationException
         session.invalidate() + SecurityContextHolder.clearContext()
         HTTP 401：{"data":{"loginUrl":"/oauth2/authorization/snack-upms-server"}}
    ⑤ 前端 requestErrorConfig.ts 识别 data.loginUrl → 显示"登录状态已过期，请重新登录"，1.5秒后跳转
    ⑥ 重新登录 → 同 §5.2 流程（密码已过期）→ 路由守卫跳转改密页
```

**检测时延**：最长 5 分钟（AT TTL）。RT rotation 已启用（`reuseRefreshTokens=false`）；`CacheSessionRefreshLock` 防止同一 Session 并发刷新造成新 RT 被覆盖。

**闭环状态**：✅

---

### 5.4 登录失败与账号锁定

```
密码错误：
    DaoAuthenticationProvider → BadCredentialsException
    OAuth2AuthenticationEvents.onAuthenticationFailure()
        LoginAttemptService.incrementFailCount(username)  [Caffeine 缓存，synchronized]
    UpmsAuthenticationEvents.onFailure() → sys_login_log: LOGIN_FAILURE

第 5 次失败 → lockUser()：
    lockCount += 1
    lockDurations=[5, 30, 0]（分钟）
        第1次 → locked 5min
        第2次 → locked 30min
        第3次 → 永久（lockUntil=null, locked=1）
    DB: oauth_user.locked=1, lockCount, lockUntil 更新
    Cache: resetFailCount()

锁定期间登录：
    LockCheckingUserDetailsService → isLocked()=true → LockedException（跳过 DB 查询）

成功登录后自动解锁：
    OAuth2AuthenticationEvents.handleLoginSuccess()
    DB: locked=0, lockCount=0, lockUntil=null；Cache 清除
```

**锁定升级**：`[5min → 30min → 永久]`，每次触发锁定 `lockCount+1` 后查 `lockDurations[lockCount-1]`，超出索引则永久锁定。

**闭环状态**：✅

---

### 5.5 用户禁用 / 账号管控

管理员通过以下接口管控账号，UPMS 服务层按需同步至 oauth_user：

| 操作      | 接口                                                     | oauth_user 副作用                                       |
|---------|--------------------------------------------------------|------------------------------------------------------|
| 禁用 / 启用 | `PUT /api/upms/users/{id}` `{status:0/1}`              | `sys_user.status` 权威，镜像同步 `oauth_user.enabled=0/1`；禁用时吊销所有 session |
| 解锁      | `PATCH /api/upms/users/{id}/unlock`                    | `locked=0, lockCount=0, lockUntil=null`              |
| 重置密码    | `POST /api/upms/users/{id}/reset-password`             | 密码更新，`initialPassword=1`，吊销所有 session                |
| 强制下线    | `DELETE /api/upms/users/{id}/tokens`                   | ① `oAuth2UserClient.revokeTokens()`：SAS 删 `oauth_authorization`（RT 立即失效）并由 `OAuthSessionInvalidator` 直接 `invalidate` SAS 侧 `HttpSession`，阻止静默重新授权 ② expire BFF Session（`SessionRegistry.expireNow()`）；细粒度踢出单个设备见 §5.8 |
| 设置到期日   | `PUT /api/upms/users/{id}` `{expireDate:"2026-12-31"}` 或 `{expireDate:null}` 清除 | `sys_user.expire_date` 镜像 + 单向同步 `oauth_user.expire_date`；登录/刷新 Token 时自动判定（§GAP-6）；清除链路经 `JsonNullable` + `setNulls` 接通（§GAP-7） |

禁用 / 吊销 session 后：已颁发 AT 最长仍有 5 分钟有效期（RS 纯 JWT 验证）。重置密码后下次登录触发 §5.2 强制改密流程。

**`status` 双层语义**：`sys_user.status` 与 `oauth_user.enabled` 同义且镜像同步，但分属两层强制：
- **登录闸门**：`enabled=0` → `UserDetailsServiceImpl` 置 `disabled` → 登录/刷新被拒（生效窗口 ≤5min，随 AT 过期）。
- **UPMS 访问闸门**：`UpmsDynamicAuthorizationManager.authorize()` 最前（早于 isAdmin）读 `sys_user.status`（`SysUserService.getStatusByUsername`，`@Cacheable(upms:user)`），禁用 → `AuthorizationDecision(false)`（403），**含被禁用的管理员，且即时生效**（不必等 AT 过期；改状态经 `@CacheEvict(upms:user)` 立即失效缓存）。

**认证侧信息聚合**：用户详情（`GET /api/upms/users/{id}` → `queryById`）与个人信息（`GET /api/upms/users/info` → `getByUsername`）统一经 `SysUserServiceImpl.attachOAuth(vo)` 调 `oAuth2UserClient.getByUsername()`，将 `oauth_user` 的 `locked`/`initialPassword`/`mfaEnabled` 等以内嵌对象 `SysUserVO.oauthVO` 返回（列表查询 `queryByDsl` 不聚合，避免 N+1）。

**闭环状态**：✅（GAP-3 已修复）

---

### 5.6 无权限访问（403）

```
情形 A：pre_auth_reset 用户访问任何非白名单接口（除改密外）
    PasswordRestrictionAuthorizationManager → 含 SCOPE_pre_auth_reset → 403

情形 B：普通用户访问无 RBAC 权限的接口（UpmsDynamicAuthorizationManager 同进程注册时）
    PasswordRestrictionAuthorizationManager → 通过
    UpmsDynamicAuthorizationManager：
        sys_user.status = 禁用 → 403（早于 ROLE_ADMIN，含被禁用的管理员，即时生效）
        ROLE_ADMIN → 直接放行
        URL 不在 sys_resource → 403（deny-by-default）
        URL 在 sys_resource 但无权限 → 403

情形 C：访问 oauth-server 管理端点（/api/oauth/user/**）
    hasAuthority("SCOPE_client")：
        注册的 client 凭证获取的 AT（自动包含 scope=client）→ 放行
        普通用户 AT（scope=openid,profile）→ 403
```

**闭环状态**：✅（`UpmsDynamicAuthorizationManager` deny-by-default；SAS 分离部署时另有 `SCOPE_client` + 网络隔离双重防线，GAP-1 已修复）

---

### 5.7 登出与 Token 撤销

```
POST /logout（前端经 /auth-api/logout 代理）
    │
    ├─ [HIGHEST_PRECEDENCE] AuditLogoutHandler
    │    sys_login_log: LOGOUT
    │
    ├─ [HIGHEST_PRECEDENCE+100] RevokeTokenLogoutHandler
    │    POST /oauth2/revoke (AT) + POST /oauth2/revoke (RT)
    │    oauth_authorization 软标记 invalidated（记录保留，OIDC logout 仍可查到）
    │    OAuth2AuthorizedClientRepository 移除记录
    │
    ├─ SecurityContextLogoutHandler
    │    SecurityContextHolder.clearContext() + session.invalidate()
    │
    └─ CsrfLogoutHandler → 清除 CSRF Cookie
    │
    JsonLogoutSuccessHandler（SAS 与 BFF 跨源时）
    HTTP 200: {"redirectUrl": "http://localhost:9000/connect/logout
                               ?post_logout_redirect_uri=<BFF_OAuth2_Start>
                               &id_token_hint=<ID_Token>"}
    前端: window.location.href = redirectUrl
    │
    SAS /connect/logout（OIDC RP-Initiated Logout）
    │    校验 id_token_hint（要求含 openid scope）
    │    校验 post_logout_redirect_uri（需在 oauth_registered_client.post_logout_redirect_uris 内）
    │    清除 OAUTH_SESSION
    │    重定向到 post_logout_redirect_uri
    │
    浏览器 → BFF /oauth2/authorization/snack-upms-server → 重新走授权码流程

JsonLogoutSuccessHandler（SAS 与 BFF 同源时）
    HTTP 200: {"redirectUrl": "/oauth2/authorization/snack-upms-server"}
    前端: window.location.href = redirectUrl → 重走授权码流程（SAS session 已由 BFF invalidate 清除）
```

**关键设计**：
- `RevokeTokenLogoutHandler` 软撤销（标记 invalidated，不删记录），保证 OIDC logout 能通过 `id_token_hint` 找到 authorization 记录
- 改密时 `OAuthUserServiceImpl.update()` **不再硬删** oauth_authorization，避免 `/connect/logout` 报 400
- 多设备踢人由管理员 `DELETE /api/upms/users/{id}/tokens` 接口负责
- **SAS 与 BFF 同源时不走 OIDC RP-Initiated Logout**：`JsonLogoutSuccessHandler` 比较 `endSessionEndpointUri` 的 origin 与当前请求的 origin，同源则直接返回 `/oauth2/authorization/{registrationId}` 跳过 `/connect/logout`。原因：同源时 SAS 与 BFF 共用 `HttpSession`，`SecurityContextLogoutHandler.session.invalidate()` 在客户端跳转前已销毁 session，SAS 收到 `/connect/logout` 时找不到关联 session 会返回 400。

**管理员强制踢人**（`DELETE /api/upms/users/{id}/tokens`，见 §5.5）：

UPMS 侧两步串行执行：
1. `oAuth2UserClient.revokeTokens(username)` → SAS 在单一接口内完成自身清理（SAS 自管理 session，UPMS 无需感知部署模式）：
   - 删 `oauth_authorization`（RT 立即失效）
   - `OAuthSessionInvalidator.invalidateByUsername()` 直接调 `HttpSession.invalidate()` 销毁 SAS 侧会话，浏览器再次携带旧 JSESSIONID 访问 `/oauth2/authorize` 时找不到 SecurityContext，SAS 跳转登录页，**阻断静默重新授权**
   - 仅处理 principal 为 `UserDetails` 的 session（SAS 本地认证产生）；principal 为 `OidcUser`/`OAuth2User` 的 BFF session 跳过不碰——SAS 与 BFF 同进程部署时两者共存于同一 JVM，BFF session 须留给步骤 2 的 `expireNow()` 处理，否则 session 被直接销毁后 `ConcurrentSessionFilter` 无法返回「已被踢下线」提示，用户只会看到 403
2. `sessionService.revokeSessionsByUsername(username)` → BFF 侧 `SessionRegistry.expireNow()`，`ConcurrentSessionFilter`（Order 2 链）下次请求返回 401 JSON，前端跳转重新登录

AT 最长仍有 5 分钟有效期窗口（RS 纯 JWT 验证，无法即时吊销）。

细粒度踢出单台设备：`DELETE /api/upms/users/{id}/sessions/{sessionId}`（见 §5.8），只踢出指定 Session，RT 不受影响。

**闭环状态**：✅

---

### 5.8 并发 Session 控制（踢出旧设备）

通过 `maxSessions` 配置项限制同一用户的同时在线设备数（默认 `-1` = 不限制）：

```yaml
snack.oauth2.client.max-sessions: 1    # 1 = 同时只允许一个设备登录
```

```
新设备登录（授权码回调成功）
    │
    │ ConcurrentSessionControlAuthenticationStrategy
    │ 检查当前用户已登录 Session 数 ≥ maxSessions
    │ → 将最旧 Session 标记为 expired（不立即销毁）
    │
    │ 旧设备下次发起任意请求：
    │   SessionManagementFilter 检测 session.isExpired()
    │   → JsonExpiredSessionStrategy
    │     HTTP 401: {"data":{"loginUrl":"..."}, "msg":"您的账号已在其他设备登录，当前会话已失效"}
    │
    │ 前端 requestErrorConfig.ts 识别 data.loginUrl → 展示提示 → 跳转重新登录
```

`msg` 字段由 `oauth2ClientMessageSource` 按请求 Locale 解析，支持 `zh_CN`、`ja_JP`、默认英语三种 locale。

**Session 注册表**：框架按 classpath 自动选择实现：

| classpath 条件                             | Bean                            | 适用场景       |
|------------------------------------------|---------------------------------|------------|
| 无 spring-session                         | `SessionRegistryImpl`           | 单节点（内存）    |
| 有 `FindByIndexNameSessionRepository` | `SpringSessionBackedSessionRegistry` | 多节点（Redis） |

**活跃 Session 查询与踢出接口**（Session 作为 user 子资源，挂在 `SysUserController` 下，复用用户管理 RBAC 权限）：

| 接口                                              | 说明                       |
|--------------------------------------------------|--------------------------|
| `GET /api/upms/users/{id}/sessions`              | 查询该用户的活跃 Session         |
| `DELETE /api/upms/users/{id}/sessions/{sessionId}` | 踢出该用户的指定 Session，立即标记过期 |

用户列表查询（`POST /api/upms/users/query`）的每行结果附带 `lastActiveTime`（该用户所有活跃 Session 的最新 lastRequest，`ConcurrentSessionFilter` 每次请求自动刷新；无活跃 Session 时为 null），可用于展示在线状态。

**闭环状态**：✅

---

### 5.9 记住我登录

勾选"记住我"后关闭浏览器重开，Session Cookie 仍存在，无需重新登录。实现采用**意图 cookie 模式**：短效 cookie 携带意图跨越 OAuth2 重定向边界，服务端在 OAuth2 回调时一次性消费并立即删除，不可重放。

```
前端登录页                                     BFF OAuth2 callback
  │                                                    │
  │ [用户勾选"记住我"]                                  │
  │ 表单提交成功，准备跳转前：                            │
  │ document.cookie = 'x-remember-me=1; max-age=300; path=/; SameSite=Strict'
  │                                                    │
  │ window.location.href = redirectUrl                 │
  │ ──── OAuth2 authorize → SAS → code 换 token ──────>│
  │                                                    │
  │                          RememberMeAwareSuccessHandler.onAuthenticationSuccess()
  │                          ① 检测到 x-remember-me=1 cookie
  │                          ② session.setMaxInactiveInterval(30 * 24 * 3600)
  │                          ③ 写持久化 session cookie（Max-Age=30天）
  │                          ④ 删除 x-remember-me cookie（Max-Age=0）
  │                          ⑤ 委托给原始 successHandler 执行重定向
  │<─────────────────────────── 302 → defaultSuccessUrl
```

**关键设计说明**：

- `RememberMeAwareSuccessHandler` 是**装饰器**，位于 `snack-oauth2-client-starter`。由 `OAuthFormLoginCustomizer.buildOAuth2SuccessHandler()` 包装原始处理器并直接注册为 `oauth2Login.successHandler()`，绕开 Customizer order 问题。
- `OAuthFormLoginCustomizer.getOrder()` = `LOWEST_PRECEDENCE`（Integer.MAX_VALUE），是 Customizer 链中**最后执行**的，其 `successHandler` 设置永远生效。因此记住我逻辑必须在此 Customizer 内处理，不能通过另一个 Customizer（如 `UpmsSecurityConfiguration`，order=-100）覆盖 successHandler——后者的设置会被 `OAuthFormLoginCustomizer` 覆盖。
- Session cookie 名称从 `ServletContext.getSessionCookieConfig().getName()` 动态读取（对应 `server.servlet.session.cookie.name: UPMS_SESSION`），不硬编码。
- 不勾选记住我：Session Cookie 无 Max-Age，关闭浏览器即消失。Session 服务端超时默认 8h（与 RT TTL 对齐）。

**安全性**：

- 意图 cookie 有效期仅 5 分钟，仅够完成授权码流程。
- 服务端读取后立即删除，不可重放。
- `SameSite=Strict` 防止 CSRF 场景下跨站自动携带。

**闭环状态**：✅

---

### 5.10 MFA 二次验证登录

当用户已在账户设置中绑定 TOTP（`oauth_user.mfa_enabled=1`）时，账密认证通过后须完成二次验证方可获得正式 Token。触发条件：`MfaVerificationRestriction.appliesTo(user)` = `mfa_enabled == YesNoStatus.YES`。

```
前端登录页                                     SAS (OAuthUserController)
  │                                                    │
  │ [用户提交账密，登录成功]                              │
  │                                                    │
  │ ① SAS 颁发受限 AT：                                │
  │   MfaVerificationRestriction.appliesTo(user) = true │
  │   UserDetailsServiceImpl → authorities=[SCOPE_pre_auth_mfa]
  │   OAuth2TokenCustomizerImpl：                       │
  │     scope={openid, pre_auth_mfa}, authorities=[], TTL=5min
  │                                                    │
  │ ② JsonAuthenticationSuccessHandler                 │
  │   返回 {"redirectUrl": "/oauth2/authorize?..."}     │
  │                                                    │
  │ window.location.href = "/oauth2/authorize?..."      │
  │──────────────────────────────────────────────────> │
  │                                                    │
  │ ③ PreAuthRestrictionFilter：含 SCOPE_pre_auth_mfa  │
  │   requestCache.saveRequest() ← 保存 /oauth2/authorize?...
  │   MfaVerificationRestriction.onApplied(username)   │
  │     MfaProvider.send() → TOTP 为 no-op             │
  │     （码由 Authenticator App 本地生成，无需服务端推送）│
  │   response.sendRedirect(frontendBaseUrl + "/account/mfa-verify")
  │<── 302 → /account/mfa-verify                       │
  │                                                    │
  │ [用户打开 Authenticator App，读取当前 6 位 TOTP 码]   │
  │ POST /oauth2/account/verify-mfa {"code":"123456"}  │
  │──────────────────────────────────────────────────> │
  │   @PreAuthorize("hasAuthority('SCOPE_pre_auth_mfa')")
  │   MfaProvider.verify(username, code)               │
  │     → oauthUserRepository 查询 mfa_secret          │
  │     → codeVerifier.isValidCode(secret, code)       │
  │       （SHA1 / 6位 / 30秒步长，dev.samstevens.totp）│
  │   验证通过 → session 就地升级（同 §5.2 改密后升级逻辑）│
  │   requestCache.getRequest() 取回 /oauth2/authorize?...
  │<── 200 {"redirectUrl": "/oauth2/authorize?..."}    │
  │                                                    │
  │ window.location.href = "/oauth2/authorize?..."      │
  │   PreAuthRestrictionFilter → session 已升级 → 跳过   │
  │   SAS 正常颁发授权码 → BFF 换取 AT/RT → 进入系统     │
```

**MFA 设备管理**：

| 接口 | 说明 |
|------|------|
| `GET /api/upms/users/mfa/setup` | 返回 TOTP 二维码及 Base32 密钥（`MfaSetupVO`），引导用户绑定 Authenticator App |
| 管理员重置 MFA | 管理员通过 UPMS 端点清除 `mfa_secret`、关闭 `mfa_enabled`，用户下次登录无需二次验证 |

**关键设计说明**：
- `MfaProvider` 是 SPI，`TotpMfaProvider` 通过 `@ConditionalOnProperty(name = "snack.mfa.type", havingValue = "TOTP", matchIfMissing = true)` 条件装配，可替换为短信/邮件 OTP 实现。
- `send()` 对 TOTP 为空实现，扩展其他类型时在此推送一次性码。
- `MfaVerificationRestriction.getOrder() = 2`，在 `PasswordChangeRestriction`（order=1）之后执行；同一登录流程两个限制互斥（先改密、改密完成后才能触发 MFA，反之亦然）。

**闭环状态**：✅

---

### 闭环状态汇总

| 场景            | 状态 | 说明                                                 |
|---------------|----|----------------------------------------------------|
| 新用户创建         | ✅  | 双写，事务保障                                            |
| 初始密码强制改密      | ✅  | 权限降级 + 5min 受限 Token                               |
| 密码过期（登录前）     | ✅  | 同初始密码改密流程                                          |
| 密码过期（会话中）     | ✅  | `SessionStateCheckFilter` + SAS 重验，GAP-8 已修复；RT rotation + 刷新锁确保无竞争 |
| 登录失败与锁定       | ✅  | 阶梯锁定，审计日志完整                                        |
| 无权限访问（403）    | ✅  | deny-by-default；SAS 分离部署时另有 `SCOPE_client` 双重防线，GAP-1 已修复 |
| 用户禁用 / 账号管控   | ✅  | 禁用/解锁/重置密码/强制下线，GAP-3 已修复                          |
| 账号到期（日期自动过期）  | ✅  | `expire_date` OR `expired=1`，登录/刷新时重验，GAP-6 已修复    |
| 权限调整实时生效      | ✅  | AT 层 ≤5min 延迟，UPMS 资源层立即生效                         |
| 登出 / Token 撤销 | ✅  | 审计 + 撤销 + Session 销毁链完整                            |
| 并发 Session 控制  | ✅  | `maxSessions` 可配置；新设备登录踢出旧会话，i18n 提示，Session 注册表自动适配内存/Redis |
| 记住我登录         | ✅  | 意图 cookie 跨 OAuth2 重定向边界；`RememberMeAwareSuccessHandler` 延长 Session 至 30 天 |
| MFA 双因素认证     | ✅  | TOTP 受限 Token + `PreAuthRestrictionFilter` + `POST /oauth2/account/verify-mfa` session 升级 |

---

## 6. 数据库关键表

### 6.1 oauth_user（用户认证信息）

| 字段                       | 类型         | 语义                                       | 正常值              |
|--------------------------|------------|------------------------------------------|------------------|
| username                 | varchar    | 主键，登录名                                   | —                |
| password                 | varchar    | DelegatingPasswordEncoder BCrypt 哈希      | —                |
| enabled                  | tinyint(1) | **Status 语义**                            | 1=启用, 0=禁用       |
| locked                   | tinyint(1) | **YesNo 语义**                             | 0=正常, 1=锁定       |
| expired                  | tinyint(1) | **YesNo 语义**                             | 0=正常, 1=过期（手动强制） |
| initial_password         | tinyint(1) | **YesNo 语义**                             | 0=否, 1=是         |
| lock_count               | int        | 锁定累计次数                                   | 0                |
| lock_until               | datetime   | 临时锁定截止；null+locked=1 = 永久                | null             |
| last_password_reset_time | datetime   | 用于 90 天过期计算                              | —                |
| expire_date              | date       | 账号到期日；null=永不到期；登录时与 `expired=1` 做 OR 判断 | null             |
| mfa_enabled              | tinyint(1) | **YesNo 语义**                             | 0=关闭, 1=已开启 MFA |
| mfa_secret               | varchar    | TOTP 密钥（Base32）；`mfa_enabled=0` 时为 null | —                |

> ⚠️ `enabled` 使用 `Status` 枚举（ENABLED=1），其余布尔字段使用 `YesNo` 枚举（NO=0=正常）。语义不统一，赋值必须使用正确的枚举常量。

### 6.2 sys_user ↔ oauth_user 字段归属与同步

`oauth_user` 为认证权威（SAS 消费），`sys_user` 为 UPMS 业务侧。字段按归属分三类：管理意图字段（UPMS 写、oauth 消费）、通道字段（双写冗余）、运行时认证状态（oauth 运行时写、不反向镜像）。

| 字段                                              | 权威 / 同步方向                                              | UPMS 展示             |
|-------------------------------------------------|-------------------------------------------------------|---------------------|
| realName / nickname / avatar / gender / birthday / remark、roleCodes / orgCodes | sys_user 自管理                                          | 本地                  |
| mobile / email                                  | 双写冗余（oauth 作改密 / 发送初始密码通道，sys_user 副本供查询）             | 本地                  |
| `status`（sys_user）↔ `enabled`（oauth_user），同义不改名 | sys_user 写 → 镜像同步 oauth；双层强制：登录走 `enabled`、UPMS 访问走 `status`（§5.5、§5.6） | 本地 `status`         |
| `expire_date`                                   | sys_user 镜像 + 单向同步 oauth；清除经 `JsonNullable`+`setNulls`（§GAP-7） | 本地 `expireDate`     |
| locked / lockUntil / lockCount、initialPassword、lastPasswordResetTime（→凭证过期）、mfaEnabled | oauth 运行时权威，**不镜像** sys_user                          | 详情聚合 `oauthVO`（unlock / reset 动作触发写） |
| password / mfaSecret                            | oauth                                                 | 永不外露（不入 `OAuthUserVO`） |
| `expired`(flag)                                 | oauth；与 `expire_date` 做 OR 判断账号到期，当前恒为 0（近似死字段）        | 详情 `oauthVO.expired` |

> `OAuthUserVO` 不含 password / mfaSecret / lockUntil / lockCount，可整体安全外露；`SysUserVO` 内嵌整个 `oauthVO` 对象（非扁平，避免与 sys_user 同名字段冲突），由 `attachOAuth` 一次性填充。

### 6.3 sys_user 关联体系

```
sys_user ──(username)──> sys_user_role ──(roleCode)──> sys_role
                                                              │(roleCode)
                                                        sys_role_resource
                                                              │(resourceId)
                                                        sys_resource
```

### 6.4 sys_login_log（登录审计）

| 字段             | 说明                                                                         |
|----------------|----------------------------------------------------------------------------|
| username       | 登录用户名                                                                      |
| action         | LOGIN_SUCCESS / LOGIN_FAILURE / LOGOUT                                     |
| failure_reason | INVALID_CREDENTIALS / ACCOUNT_LOCKED / ACCOUNT_DISABLED / PASSWORD_EXPIRED |
| ip_address     | 客户端 IP（支持 X-Forwarded-For）                                                 |
| user_agent     | 浏览器 UA（最长 500 字符截断）                                                        |
| session_id     | HTTP Session ID                                                            |

---

## 7. 部署模式

### 7.1 合并部署 vs 分离部署

| 维度                      | 分离部署（正式目标）                               | 合并部署（当前开发）                              |
|-------------------------|------------------------------------------|-----------------------------------------|
| 进程数                     | 2：snack-oauth-server + snack-upms-server | 1：snack-upms-server 同时承担 SAS + BFF + RS |
| `OAUTH_SERVER_URL`      | 指向独立 oauth-server                        | dev profile 下默认 `http://localhost:9000`；非 dev 须显式设置 |
| `OAUTH_LOGIN_PAGE`      | 指向前端登录页绝对 URL                            | dev profile 下默认 `http://localhost:8000/user/login`；非 dev 须显式设置 |
| `DefaultSecurityConfig` | 在 oauth-server 进程生效（无 Client Starter）    | 不生效（Order 2 Client 链已存在）                |
| OAuth2UserClient 调用     | 跨进程 HTTP                                 | 进程内 loopback HTTP（仍需携带有效 Bearer Token）  |

> **代码中不存在「部署模式」标志或判断**。两种拓扑的行为差异全部由环境自然涌现：Bean 存在性（`@ConditionalOnMissingBean`）、classpath（`compileOnly` 依赖）、origin 比较（`JsonLogoutSuccessHandler`）、principal 类型（`OAuthSessionInvalidator`）。新增代码也不得引入部署模式开关。

### 7.2 各进程安全链

**分离部署**：
```
snack-oauth-server
├─ Order 1: AuthorizationServerSecurityFilterChain
└─ Order 2: DefaultSecurityConfig（form-login + /api/oauth/user/** 保护）

snack-upms-server
├─ Order 2: OAuth2ClientSecurityFilterChain
└─ Order 3: ResourceServerSecurityFilterChain
```

**合并部署**：
```
snack-upms-server
├─ Order 1: AuthorizationServerSecurityFilterChain
├─ Order 2: OAuth2ClientSecurityFilterChain（DefaultSecurityConfig 因 Bean 存在而不生效）
└─ Order 3: ResourceServerSecurityFilterChain
```

### 7.3 切换方式

**合并 → 分离**：
1. 部署独立 `snack-oauth-server`（端口 9000）
2. 设置 `OAUTH_SERVER_URL=http://<oauth-server-host>:<port>`
3. 确认 `redirect_uris` 回调地址指向 upms-server
4. （可选）从 `snack-upms-server/build.gradle` 移除 `snack-oauth-biz` 依赖，减少不必要的 Bean 初始化

**分离 → 合并**：取消设置 `OAUTH_SERVER_URL`，确保 `snack-upms-server` 引入 `snack-oauth-biz` 依赖。

> 切换的唯一必要操作是 `OAUTH_SERVER_URL`，不涉及代码改动。`snack-upms-server` 内嵌的 SAS 端点在分离部署时依然启动，但所有 OAuth2 流量走 `OAUTH_SERVER_URL` 指向的外部服务，本地 SAS 不会收到用户流量。

---

## 8. 关键配置速查

### 安全策略（前缀 `snack.security.policy`）

```yaml
snack:
  security:
    policy:
      password-expiration-days: 90        # ≤0 禁用密码过期检查
      max-login-attempts: 5               # 第5次失败触发锁定
      lock-durations: [5, 30, 0]          # 阶梯锁定（分钟），0=永久
      force-change-initial-password: true
      default-password: Snack@123
      login-page: ${OAUTH_LOGIN_PAGE:/user/login}    # SAS 未认证时重定向的登录页；frontendBaseUrl 从此自动提取
      permit-all-paths:                              # 无需认证的路径（默认值）
        - /error
        - /actuator/health
      csrf-ignore-paths:                             # CSRF 豁免路径（默认值）
        - /login
        - /oauth2/account/**
      logout-url: /logout                            # 触发登出的路径（默认值）
      pre-auth-pages:
        pre_auth_reset: /account/change-password     # PasswordChangeRestriction 对应的前端处理页
        pre_auth_mfa: /account/mfa-verify            # MfaVerificationRestriction 对应的前端处理页
```

### MFA 配置（前缀 `snack.mfa`）

```yaml
snack:
  mfa:
    type: TOTP    # MFA 类型（当前仅支持 TOTP）；条件装配 TotpMfaProvider 和 CodeVerifier Bean
                  # 不配置或配置为 TOTP 时默认启用（matchIfMissing=true）
```

> `login-page` 通过环境变量 `OAUTH_LOGIN_PAGE` 注入（生产/测试环境），dev profile 下由 `application-oauth.yml` 多文档块自动覆盖为 `http://localhost:8000/user/login`，无需手动设置。`frontendBaseUrl` 无需显式配置——`SecurityProperties.getFrontendBaseUrl()` 自动从 `login-page` 提取 scheme + host + port。

### Session 配置（`snack-upms-server/src/main/resources/application.yml`）

```yaml
server:
  servlet:
    session:
      timeout: 8h              # 与 SAS RT TTL 对齐；不勾记住我时 8h 无操作自动过期
      cookie:
        name: UPMS_SESSION     # RememberMeAwareSuccessHandler 通过 getSessionCookieConfig().getName() 读取此值
```

> **记住我**：勾选后 `RememberMeAwareSuccessHandler` 将 `session.setMaxInactiveInterval(30天)` 并写入 `Max-Age=30天` 的持久化 session cookie，覆盖容器的 session-only cookie。SAS RT TTL 仍为 8 小时，活跃用户每次 AT 刷新触发 RT rotation（重置 8h 时钟）；超过 30 天不活跃（含 8h 无请求触发 RT 过期）时，服务端 Session 或 RT 先到期均会触发重新登录。

### UPMS 客户端（`application-upms.yml`）

```yaml
snack:
  oauth2:
    client:
      server-url: ${OAUTH_SERVER_URL}                  # 必须设置；dev profile 下默认 http://localhost:9000
      login-page: ${OAUTH_LOGIN_PAGE:/user/login}      # OAuth2 客户端侧登录页；dev 下自动覆盖为绝对 URL
      default-success-url: ${OAUTH_DEFAULT_SUCCESS_URL:/}  # 登录成功跳转；dev 下覆盖为 http://localhost:8000/
      max-sessions: -1                                 # 并发 Session 数，-1 不限制，1 = 单设备登录
      permit-all-paths:                                # 无需认证的路径（默认值）
        - /error
        - /actuator/health
        - /logout
        - /login/oauth2/**
      csrf-ignore-paths:                               # CSRF 豁免路径（默认值）
        - /login
        - /logout
      api-path-pattern: /api/                          # 未认证时返回 401 JSON 的路径前缀（默认值）

spring:
  security:
    oauth2:
      client:
        registration:
          snack-upms-server:                    # BFF 授权码客户端
            authorization-grant-type: authorization_code
            scope: openid,profile
          snack-upms-service:                   # 服务账号（UPMS → oauth admin 接口）
            authorization-grant-type: client_credentials
            scope: upms
```

> 所有 localhost 地址均通过环境变量注入；dev profile 下各模块 `application-{profile}.yml` 多文档块自动提供默认值，无需手动配置环境变量。

### Token 配置（`oauth_registered_client.token_settings`）

```json
{
  "settings.token.access-token-time-to-live":  "PT5M",
  "settings.token.refresh-token-time-to-live": "PT8H",
  "settings.token.reuse-refresh-tokens":       false
}
```

---

## 9. HTTP 响应速查

### 响应格式

```
{ "code": null,    "msg": "Success",   "data": {...} }
{ "code": "2003",  "msg": "...",        "data": null  }
{ "code": "2003",  "msg": "...",        "data": {"loginUrl": "..."} }
```

**错误码**：

| 错误码    | 含义              |
|--------|-----------------|
| `null` | 成功              |
| `1000` | 系统内部错误          |
| `1001` | 外部接口调用异常        |
| `1002` | 请求参数无效          |
| `1003` | 资源不存在           |
| `2000` | 数据已存在           |
| `2001` | 数据不存在           |
| `2002` | 数据状态错误          |
| `2003` | 权限不足（未认证 / 无权限） |
| `2004` | 操作不允许           |

---

### A. 表单登录（POST /auth-api/login → 代理到 POST /login）

| 场景                 | Status  | 响应体                                                      | 处理器                                |
|--------------------|---------|----------------------------------------------------------|------------------------------------|
| 登录成功               | **200** | `{"code":null,"data":{"redirectUrl":"<savedUrl或null>"}}` | `JsonAuthenticationSuccessHandler` |
| 失败（密码错误 / 锁定 / 禁用） | **401** | `{"code":"2003","msg":"<Spring i18n 消息>"}`               | `JsonAuthenticationFailureHandler` |

> `redirectUrl` 来自 `HttpSessionRequestCache`（登录前被拦截的 OAuth2 授权请求 URL）。前端拿到后执行 `window.location.href = redirectUrl`，完成完整授权码流程；若为 `null` 则前端回退到安全校验后的 redirect 参数或根路径。

---

### B. OAuth2 授权码回调（GET /login/oauth2/code）

| 场景   | Status  | 响应                                                 | 处理器                                     |
|------|---------|----------------------------------------------------|-----------------------------------------|
| 授权成功 | **302** | 重定向至 `defaultSuccessUrl`（`http://localhost:8000/`） | `SimpleUrlAuthenticationSuccessHandler` |
| 授权失败 | **302** | 重定向至 `/login?error`                                | Spring 默认                               |

---

### C. API 访问（Session Cookie）

**C-1. 已登录用户**

| 场景                                | Status  | 响应体                                                               | 处理器                                       |
|-----------------------------------|---------|-------------------------------------------------------------------|-------------------------------------------|
| 有权限                               | **200** | 业务数据                                                              | Controller                                |
| 已认证但无 RBAC 权限                     | **403** | `{"code":"2003","msg":"Access Denied"}`                           | `BizAccessDeniedHandler`                  |
| `pre_auth_reset` 用户访问非 profile 接口 | **403** | `{"code":"2003","msg":"Access Denied"}`                           | `PreAuthAuthorizationManager`             |
| AT 未过期，会话中密码到期（5min 窗口内）          | **200** | 业务数据（缓存 AT 有效）                                                    | Controller                                |
| AT 过期，SAS 刷新时检测到密码 / 账号异常         | **401** | `{"data":{"loginUrl":"/oauth2/authorization/snack-upms-server"}}` | `SessionStateCheckFilter`                 |

**C-2. 未登录 / Session 超时**

| 请求类型           | Status  | 响应体                                                                             | 处理器                                |
|----------------|---------|---------------------------------------------------------------------------------|------------------------------------|
| `/api/**` AJAX | **401** | `{"code":"2003","data":{"loginUrl":"/oauth2/authorization/snack-upms-server"}}` | `BizAuthenticationEntryPoint`      |
| 浏览器直接导航        | **302** | — 重定向至 OAuth2 授权端点                                                              | `LoginUrlAuthenticationEntryPoint` |

> 前端 `requestErrorConfig.ts` 收到 401 含 `data.loginUrl` 时展示"登录状态已过期，请重新登录"提示，1.5 秒后跳转。**前端不硬编码授权端点**，地址由后端响应动态提供。

---

### D. Bearer Token 访问（携带 Authorization: Bearer 头）

| 场景               | Status  | 响应格式                                                            | 处理器                                   |
|------------------|---------|-----------------------------------------------------------------|---------------------------------------|
| Token 无效 / 过期    | **401** | Spring 原生：`{"error":"invalid_token","error_description":"..."}` | `BearerTokenAuthenticationEntryPoint` |
| Token 有效，RBAC 拒绝 | **403** | ApiResponse：`{"code":"2003","msg":"Access Denied"}`             | `BizAccessDeniedHandler`              |

> Bearer 请求的 Token 层 401 格式与 Session 请求不同（Spring 原生 vs ApiResponse），是现有不一致点之一。

---

### E. 登出（POST /auth-api/logout → 代理到 POST /logout）

| 场景    | Status  | 响应体                                                                                           | 处理器                        |
|-------|---------|-----------------------------------------------------------------------------------------------|----------------------------|
| 登出（SAS 与 BFF 同源） | **200** | `{"redirectUrl": "/oauth2/authorization/snack-upms-server"}`                                  | `JsonLogoutSuccessHandler` |
| 登出（SAS 与 BFF 跨源） | **200** | `{"redirectUrl": "http://SAS/connect/logout?post_logout_redirect_uri=...&id_token_hint=..."}` | `JsonLogoutSuccessHandler` |

---

### F. 应用层异常（Controller 层，`@RestControllerAdvice`）

| 场景                                             | Status  | `code`   |
|--------------------------------------------------|---------|----------|
| 参数校验失败                                       | **400** | `"1002"` |
| 路径不存在                                         | **404** | `"1003"` |
| 业务异常（`BusinessException`）                    | **422** | 异常携带的错误码 |
| 下游业务拒绝（`InterfaceBusinessException`）        | **422** | `"1001"` |
| 下游网络 / 服务器异常（`InterfaceException`）        | **500** | `"1001"` |
| 系统错误                                           | **500** | `"1000"` |

---

## 10. 关键源码位置

| 组件                      | 文件路径                                                                            | 关键方法                                                          |
|-------------------------|---------------------------------------------------------------------------------|---------------------------------------------------------------|
| **SAS**                 |                                                                                 |                                                               |
| 用户自助改密（SAS）             | `snack-oauth-biz/.../controller/OAuthUserController.java`                       | `changePassword()`（改密 + session 升级 + 取 savedRequest 返回 redirectUrl） |
| MFA 二次验证（SAS）            | `snack-oauth-biz/.../controller/OAuthUserController.java`                       | `verifyMfa()`（`POST /oauth2/account/verify-mfa`，TOTP 验证 + session 升级） |
| MFA 设备绑定（UPMS）           | `snack-upms-biz/.../controller/SysUserController.java`                          | `getMfaSetup()`（`GET /api/upms/users/mfa/setup`，生成 TOTP 二维码与 Base32 密钥） |
| MFA 提供者接口                 | `snack-oauth-biz/.../security/mfa/MfaProvider.java`                             | `send(username)`（推送一次性码）, `verify(username, code)` |
| TOTP MFA 实现                 | `snack-oauth-biz/.../security/mfa/TotpMfaProvider.java`                         | `verify()`（读 `mfa_secret` + `CodeVerifier.isValidCode()`） |
| MFA 验证限制策略               | `snack-oauth-biz/.../security/restriction/MfaVerificationRestriction.java`      | `appliesTo()`（`mfa_enabled=1` 判断）, `getOrder()=2` |
| SAS 安全链配置               | `snack-oauth-biz/.../security/config/AuthorizationServerConfig.java`            | `authorizationServerSecurityFilterChain()`（注册 `PreAuthRestrictionFilter`） |
| pre-auth 限制接口           | `snack-oauth-biz/.../security/PreAuthRestriction.java`                          | `getAuthority()`, `getPagePath()`, `appliesTo()`              |
| pre-auth 限制过滤器           | `snack-oauth-biz/.../security/PreAuthRestrictionFilter.java`                    | `doFilterInternal()`（拦截 `/oauth2/authorize`，保存请求，重定向至限制页）     |
| 强制改密限制策略               | `snack-oauth-biz/.../security/restriction/PasswordChangeRestriction.java`       | `appliesTo()`（初始密码 / 密码过期检测）                                  |
| 默认安全配置（分布式）             | `snack-oauth-biz/.../security/config/DefaultSecurityConfig.java`                | `defaultSecurityFilterChain()`                                |
| JWT Token 定制器           | `snack-oauth-biz/.../security/config/OAuth2TokenCustomizerImpl.java`            | `customize()`（权限降级 + refresh_token 重验用户状态）                    |
| UserDetailsService      | `snack-oauth-biz/.../service/impl/UserDetailsServiceImpl.java`                  | `loadUserByUsername()`, `getAuthorities()`（委托 `List<PreAuthRestriction>` 检测） |
| 缓存锁定检查                  | `snack-oauth-biz/.../security/LockCheckingUserDetailsService.java`              | `loadUserByUsername()`                                        |
| 认证事件（锁定）                | `snack-oauth-biz/.../security/OAuth2AuthenticationEvents.java`                  | `onAuthenticationFailure/Success()`                           |
| 安全策略配置                  | `snack-oauth-biz/.../security/config/SecurityProperties.java`                   | `getFrontendBaseUrl()`（自动从 `loginPage` 提取）, `isCredentialsExpired()` |
| OAuth2 用户服务             | `snack-oauth-biz/.../service/impl/OAuthUserServiceImpl.java`                    | `create()`, `update()`, `revokeTokens()`（删 authorization + 失效 SAS session） |
| SAS Session 失效器          | `snack-oauth-biz/.../security/OAuthSessionInvalidator.java`                     | `invalidateByUsername()`（session 事件维护映射，直接 invalidate principal 为 `UserDetails` 的 SAS HttpSession，跳过 BFF session） |
| **OAuth2 Client / BFF** |                                                                                 |                                                               |
| Client 自动配置             | `snack-oauth2-client-starter/.../config/OAuth2ClientAutoConfiguration.java`     | `oauth2ClientSecurityFilterChain()`                           |
| AT 懒刷新过滤器               | `snack-oauth2-client-starter/.../security/SessionStateCheckFilter.java`         | `doFilterInternal()`（AT 刷新 + 异常时强制登出 401）                     |
| AT 刷新并发锁                 | `snack-oauth2-client-starter/.../security/CacheSessionRefreshLock.java`         | `tryLock()`, `unlock()`（Spring Cache 实现，Caffeine/Redis 自适配）      |
| 并发 Session 过期处理器        | `snack-oauth2-client-starter/.../security/JsonExpiredSessionStrategy.java`      | `onExpiredSessionDetected()`（并发踢出时返回 401 JSON，含 i18n 提示）       |
| Token 撤销登出处理器           | `snack-oauth2-client-starter/.../security/RevokeTokenLogoutHandler.java`        | `logout()`                                                    |
| 审计登出处理器                 | `snack-oauth2-client-starter/.../security/AuditLogoutHandler.java`              | `logout()`                                                    |
| 登出成功处理器                 | `snack-oauth2-client-starter/.../security/JsonLogoutSuccessHandler.java`        | `onLogoutSuccess()`                                           |
| 记住我成功处理器装饰器             | `snack-oauth2-client-starter/.../security/RememberMeAwareSuccessHandler.java`   | `onAuthenticationSuccess()`（读取意图 cookie → 延长 Session → 删 cookie → 委托） |
| OAuth2 安全规则             | `snack-oauth-biz/.../security/OAuth2SecurityCustomizer.java`                    | `configureAuthorization()`（Order 0）                           |
| 表单登录注入                  | `snack-oauth-biz/.../security/config/OAuthFormLoginCustomizer.java`             | `customize(http)`（Order=LOWEST，路径分派 EntryPoint，`SimpleUrlAuthenticationSuccessHandler` 包装 `RememberMeAwareSuccessHandler`） |
| 登录成功处理器                 | `snack-oauth-biz/.../security/handler/JsonAuthenticationSuccessHandler.java`    | `onAuthenticationSuccess()`（返回 redirectUrl JSON）              |
| 登录失败处理器                 | `snack-oauth-biz/.../security/handler/JsonAuthenticationFailureHandler.java`    | `onAuthenticationFailure()`（返回 401 JSON）                      |
| **Resource Server**     |                                                                                 |                                                               |
| RS 自动配置                 | `snack-oauth2-resource-server-starter/.../ResourceServerAutoConfiguration.java` | —                                                             |
| pre-auth 限制授权管理器        | `snack-oauth-biz/.../security/PreAuthAuthorizationManager.java`                 | `authorize()`（Order 1，持有任意 pre-auth 权限 → 403）                 |
| RBAC 授权管理器              | `snack-upms-biz/.../security/UpmsDynamicAuthorizationManager.java`              | `authorize()`（Order 100，deny-by-default）                      |
| UPMS 权限映射器              | `snack-upms-biz/.../security/UpmsGrantedAuthoritiesMapper.java`                 | `mapAuthorities()`（OAuth2 回调时加载权限）                            |
| 401 处理器                 | `snack-oauth-biz/.../security/handler/BizAuthenticationEntryPoint.java`         | `commence()`（`/api/**` 未认证返回 JSON）                            |
| 403 处理器                 | `snack-oauth-biz/.../security/handler/BizAccessDeniedHandler.java`              | `handle()`                                                    |
| **UPMS 业务层**            |                                                                                 |                                                               |
| UPMS 用户服务               | `snack-upms-biz/.../service/impl/SysUserServiceImpl.java`                       | `create()`, `update()`                                        |
| 活跃 Session 端点           | `snack-upms-biz/.../controller/SysUserController.java`                          | `getSessions()`, `revokeSession()`（`/api/upms/users/{id}/sessions`） |
| 活跃 Session 服务           | `snack-upms-biz/.../service/impl/SysSessionServiceImpl.java`                    | `getSessions()`, `revokeSession()`, `getLastActiveTimes()`    |
| UPMS 安全 Customizer      | `snack-upms-biz/.../security/config/UpmsSecurityConfiguration.java`             | `authoritiesMapper()`, `configureAuthorization()`（info/resources 白名单）|
| UPMS 审计事件               | `snack-upms-biz/.../security/UpmsAuthenticationEvents.java`                     | `onSuccess()`, `onFailure()`                                  |
| **前端**                  |                                                                                 |                                                               |
| 初始化状态 & 路由守卫            | `src/app.tsx`                                                                   | `getInitialState()`, `onPageChange`（改密拦截）                     |
| 错误处理                    | `src/requestErrorConfig.ts`                                                     | `errorHandler`（BizError skip / 非业务全局处理）                       |
| 认证服务                    | `src/services/auth/index.ts`                                                    | `login()`, `logout()`, `getSysUserInfo()`, `changePassword()` |
| 登录页                     | `src/pages/account/login/index.tsx`                                             | —                                                             |
| 强制改密页                   | `src/pages/account/change-password/index.tsx`                                   | —                                                             |
| 账户设置页                   | `src/pages/account/settings/index.tsx`                                          | —                                                             |

---

## 11. 已知缺口

### ~~BUG-1~~：`expired` 字段枚举语义混用 ✅ 已修复

`expired` 与 `locked` 语义对称（0=正常，1=异常），但代码曾用 `Status.DISABLED=0` 做判断导致逻辑相反。

| 位置                                            | 修复前                                                  | 修复后                                                  |
|-----------------------------------------------|------------------------------------------------------|------------------------------------------------------|
| `UserDetailsServiceImpl.loadUserByUsername()` | `accountExpired(equals(expired, Status.DISABLED=0))` | `accountExpired(equals(expired, YesNoStatus.YES=1))` |
| `OAuthUserServiceImpl.create()`               | `setExpired(Status.ENABLED=1)`                       | `setExpired(YesNoStatus.NO=0)`                       |
| `OAuthUserServiceImpl.update()`               | `setExpired(Status.ENABLED=1)`                       | `setExpired(YesNoStatus.NO=0)`                       |

---

### ~~GAP-1~~：独立 oauth-server 管理端点防护不足 ✅ 已修复

1. ✅ 注册服务账号 `snack-upms-service`（client_credentials，scope=client）
2. ✅ `OAuth2UserClientConfiguration` 改用 `OAuth2ClientHttpRequestInterceptor`，固定使用 `snack-upms-service` AT，与用户 AT 完全解耦
3. ✅ `OAuth2SecurityPolicy` 收紧：`/api/oauth/user/**` 改为 `hasAuthority("SCOPE_client")`

---

### ~~GAP-2~~：密码在活跃会话中过期时前端无法感知 ✅ 已修复

1. ✅ `OAuth2TokenCustomizerImpl`（SAS）：`refresh_token` grant 时重新从 DB 加载用户状态，`getAuthorities()` 中检测时间过期，状态异常则拒绝续发
2. ✅ `SessionStateCheckFilter`（BFF，框架层）：每次已认证请求触发 AT 懒刷新，捕获 `OAuth2AuthorizationException` 后强制 Session 失效 + 401

检测时延：最长 5 分钟（AT TTL）。

---

### ~~GAP-3~~：UPMS 后台与 oauth_user 同步缺失 / 无强制下线接口 ✅ 已修复

1. ✅ `sys_user` 增加 `status`/`mobile`/`email` 字段，`SysUser`/`SysUserDTO`/`SysUserVO` 同步增加
2. ✅ `OAuth2UserDTO` 增加 `enabled`/`locked`/`initialPassword` 字段
3. ✅ `SysUserServiceImpl.update()` 按字段变化（status/mobile/email）按需调用 `oAuth2UserClient.update()` 同步
4. ✅ oauth-server 增加 `DELETE /api/oauth/user/{username}/tokens`，按 `principal_name` 删除 `oauth_authorization` 记录（RT 立即失效）
5. ✅ UPMS 新增 `PATCH /{id}/unlock`、`POST /{id}/reset-password`（`initialPassword=1` + 吊销 session）、`DELETE /{id}/tokens` 管控端点

---

### ~~GAP-4~~：Refresh Token 轮换 ✅ 已修复

1. ✅ `reuse-refresh-tokens` 改为 `false`，每次 AT 刷新生成新 RT（旧 RT 立即失效）
2. ✅ `CacheSessionRefreshLock`（`SessionRefreshLock` SPI）防止同一 Session 并发刷新导致 RT 竞争覆盖：基于 Spring Cache，Caffeine 单节点 / Redis 分布式自动适配；持锁超时 3 秒后降级放行，不阻塞用户
3. ✅ RT TTL 延长至 8 小时，弥补 rotation 带来的有效期缩短

---

### ~~GAP-5~~：密码复杂度无后端校验 ✅ 已修复

1. ✅ 在 `SysUserDTO` 的 `password` 字段上，已通过 `@Size(min = 8, max = 255)` 与 `@Pattern` 正则注解（`^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,255}$`），在后端强行实施了高强度的密码复杂度校验（必须混合大小写字母、数字及特殊符号）。在 Controller 层由 `@Validated` 触发，完全杜绝了弱密码入库的安全隐患。

---

### ~~GAP-6~~：账号自动到期机制缺失 ✅ 已修复

1. ✅ `oauth_user` 增加 `expire_date date` 字段（null=永不到期）
2. ✅ `UserDetailsServiceImpl.loadUserByUsername()` 的 `accountExpired` 改为 OR 逻辑：`expired=1` OR `expireDate < today`；无需定时任务，登录时及 refresh_token 重验时自然感知
3. ✅ `OAuthUserDTO` / `SysUserDTO` 均以 `JsonNullable<LocalDate>` 持有 `expireDate`，管理员通过 `PUT /api/upms/users/{id}` 设置并透传至 oauth_user；清除链路已接通（见 GAP-7）

---

### GAP-7：可空字段无法通过更新接口清除 🚧 进行中

已选用 `JsonNullable<T>` 方案区分三态（未传 / 显式 `null` / 有值），基础设施已落地，`expire_date` 清除链路已接通；`mobile` / `email` 因属双写通道字段（§6.2），按业务保留普通字段、暂不清除。

**已落地（基础设施）**：
1. ✅ `WebAutoConfiguration` 注册 `JsonNullableJackson3Module`，反序列化保留"未传"与"显式 null"的区分
2. ✅ `updateByDsl(entity, UpdateCondition.builder().setNulls(dto)...)`：`WrapperBuilder` 经 `JsonNullableSupport.clearedFields(dto)` 将"显式 null"字段生成 `SET col = NULL`（已在 `SysUserDTO` 的 `avatar`/`birthday`/`remark`/`roleCodes` 等 **sys_user 本地列**验证可用）
3. ✅ `expire_date` 清除接通：`OAuthUserDTO.expireDate` 改为 `JsonNullable<LocalDate>`，oauth 侧 `OAuthUserServiceImpl.update()` 通用分支走 `setNulls(dto)`，`PUT /api/upms/users/{id}` `{expireDate:null}` 同时清除 sys_user 与 oauth_user 两侧

**未接通（按业务搁置）**：

| 字段                | 所在表                   | 现状                                                                       |
|-------------------|-----------------------|--------------------------------------------------------------------------|
| `mobile` / `email` | oauth_user / sys_user | 作改密 / 发送初始密码通道（§6.2），双写冗余但不提供清除；`SysUserDTO` / `OAuthUserDTO` 仍为普通字段，未包装 `JsonNullable` |

> 相比原候选方案（`clearFields` 列表 / 专用端点 / JSON Merge Patch），`JsonNullable` 在保留 `PUT` 接口与 JSON 语义的前提下解决三态区分，无需新增字段或切换 HTTP 方法。

---

### ~~GAP-8~~：SessionStateCheckFilter 401 响应格式与分布式登出问题 ✅ 已修复

本 GAP 涵盖两个关联修复：

**修复 1 — SessionStateCheckFilter 响应格式**
`SessionStateCheckFilter` 响应改为包含 `data.loginUrl` 的标准结构，前端 `requestErrorConfig.ts` 能正确读取并跳转：
```json
{ "data": { "loginUrl": "/oauth2/authorization/snack-upms-server" } }
```

**修复 2 — 改密后登出（OIDC RP-Initiated Logout，SAS 与 BFF 跨源时）**

原问题链：改密 → `OAuthUserServiceImpl` 硬删 `oauth_authorization` → BFF 调 `/connect/logout` → Spring AS 找不到 authorization → 400。

完整修复：
1. `OAuth2TokenCustomizerImpl`：受限 Token scope 保留 `openid`（`["openid","pre_auth_reset"]`），ID Token 符合 OIDC 规范，`/connect/logout` 的 `id_token_hint` 校验通过
2. `OAuthUserServiceImpl.update()`：改密时**不再硬删** oauth_authorization，由 BFF 的 `RevokeTokenLogoutHandler` 软撤销（标记 invalidated，记录保留）
3. `JsonLogoutSuccessHandler`：SAS 与 BFF 跨源时构造 OIDC 标准登出 URL（`/connect/logout?post_logout_redirect_uri=...&id_token_hint=...`）
4. `oauth_registered_client.post_logout_redirect_uris`：注册允许的登出回调 URI（`http://localhost:8080/oauth2/authorization/snack-upms-server`），Spring AS 7.x 强制校验此字段

---

### GAP-9：LoginAttemptService 单节点内存，多节点部署失效

**当前实现**：`LoginAttemptServiceImpl` 使用 Spring Cache（单节点 Caffeine，`expireAfterWrite=10m`）维护失败计数和临时锁定状态。

**多节点问题**：攻击者轮询不同节点可绕过失败次数限制——节点 A 记 4 次失败，节点 B 记 0 次，第 5 次打到节点 B 不触发锁定。DB 侧锁定（`oauth_user.locked/lockUntil`）是持久的，但到达锁定阈值的那次计数本身可被绕过。

**改造方案**：

```
方案 A（最小改动，推荐）：Redis 替换 Caffeine Cache
  条件：classpath 已有 spring-data-redis（Session/CacheSessionRefreshLock 共用）

  1. 将 spring.cache.type 改为 redis（或保持 auto，Redis 在 classpath 时自动选择）
  2. 配置 loginFailCount / loginLockStatus 两个 cache 的 TTL：
       spring.cache.redis.time-to-live: 10m  # 全局 TTL
     或通过 RedisCacheManagerBuilderCustomizer 单独配置
  3. LoginAttemptServiceImpl 无需任何代码改动（依赖 Spring Cache 抽象）

  ⚠️ synchronized 关键字在分布式环境无效：
     需改为 Redis 原子操作，将 incrementFailCount 改用 RedisTemplate.opsForValue().increment()
     或引入 RedissonClient 分布式锁

方案 B（强一致）：所有失败计数直接写 DB
  优点：无额外中间件依赖；缺点：高频登录失败场景下 DB 写压力上升

当前状态：待实施。单节点部署时不受影响。
```

---

### GAP-10：OAuthSessionInvalidator 单节点内存，多节点部署失效

**当前实现**：`OAuthSessionInvalidator` 用 `ConcurrentHashMap<String, HttpSession>` 维护 sessionId→HttpSession 映射，`invalidateByUsername()` 只能使当前 JVM 内的 session 失效。

**多节点问题**：管理员踢人（`DELETE /api/upms/users/{id}/tokens`）触发 `revokeTokens()` → `OAuthSessionInvalidator.invalidateByUsername()`，但用户的 SAS 侧 session 可能在另一台节点，本节点 HashMap 中找不到，session 依然有效，用户可在 SAS 静默重新授权获得新 AT/RT。

**改造方案**：

```
方案 A（推荐）：Spring Session + FindByIndexNameSessionRepository
  将 SAS 侧 session 存储迁移至 Redis（Spring Session）：

  1. 引入 spring-session-data-redis（BFF 侧可能已引入）
  2. SAS 安全链开启 Spring Session：在 AuthorizationServerConfig 中加
       http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
     并确保 HttpSessionSecurityContextRepository 使用 Spring Session 的实现
  3. 替换 OAuthSessionInvalidator 实现：
       @Autowired FindByIndexNameSessionRepository<? extends Session> sessionRepo;

       public void invalidateByUsername(String username) {
           sessionRepo.findByPrincipalName(username)
               .forEach((id, session) -> sessionRepo.deleteById(id));
       }
     无需监听 HttpSessionCreatedEvent / HttpSessionDestroyedEvent，
     sessionRepo 本身是跨节点的分布式视图

  ⚠️ 注意合并部署场景：SAS 与 BFF 共用同一 Spring Session Store 时，
     findByPrincipalName 会同时返回 principal=UserDetails（SAS）和 principal=OidcUser（BFF）的 session。
     必须保留 matchesUsername() 中 principal instanceof UserDetails 的过滤，
     只 delete SAS session，BFF session 留给 SessionRegistry.expireNow() 处理（步骤 2 串行执行，见 §5.7）。

方案 B（轻量）：Redis Pub/Sub 广播失效信号
  不迁移 session 存储，仅通过广播协调各节点本地 HashMap：

  1. invalidateByUsername() 同时 publish 到 Redis channel（key = username）
  2. 每个节点订阅该 channel，收到信号后调用本地 invalidateLocal(username)
  3. 原 ConcurrentHashMap 逻辑保持不变，只在集群广播层面新增 RedisMessageListenerContainer

  ⚠️ Pub/Sub 不保证送达（节点宕机时消息丢失），适合"最终一致"容忍度较高的场景

当前状态：待实施。单节点部署时不受影响。
```
