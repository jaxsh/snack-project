# 认证授权架构文档

> 审计日期：2026-05-24 | 最后更新：2026-06-05（前端认证流程重构、API 路径统一、GAP-8 新增）
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

**部署**：`snack-oauth-biz`。单体模式内嵌于 `snack-upms-server`；分布式模式为独立 `snack-oauth-server` 进程。

**安全链**：Order 1，`AuthorizationServerSecurityFilterChain`，匹配 `/oauth2/**`、`/.well-known/**`。

**职责**：

- 执行 OAuth2 授权码流程，验证用户凭证（委托 `LockCheckingUserDetailsService` → `UserDetailsServiceImpl`）
- 颁发 JWT AccessToken / RefreshToken / ID Token，通过 `JdbcOAuth2AuthorizationService` 持久化
- 通过 `OAuth2TokenCustomizerImpl` 注入业务语义：
  - `authorization_code` grant：检测初始密码/密码过期 → 权限降级为 `pre_auth_reset`，TTL 压缩至 5 分钟
  - `refresh_token` grant：重新从 DB 加载用户状态，状态异常（密码过期/禁用/锁定）则拒绝续发
- 支持 Token 撤销（`/oauth2/revoke`）

**关键端点**：

| 端点 | 作用 |
|------|------|
| `GET /oauth2/authorize` | 授权码流程入口，重定向至登录页 |
| `POST /login` | 表单登录（前端 SPA 经 `/auth-api/login` 代理访问） |
| `POST /oauth2/token` | 授权码 / refresh_token 换 AT |
| `GET /oauth2/jwks` | RSA 公钥，Resource Server 用于验签 |
| `POST /oauth2/revoke` | 撤销 AT 或 RT |
| `GET /userinfo` | OIDC 用户信息 |

---

### 1.2 OAuth2 Client / BFF

**部署**：`snack-oauth2-client-spring-boot-starter`（自动配置），挂载于 `snack-upms-server`。

**安全链**：Order 2，`OAuth2ClientSecurityFilterChain`，匹配所有请求。

**职责**：

- 执行授权码流程，与 SAS 交换 AT/RT，存入服务端 `HttpSession`
- **前端只持有 Session Cookie，从不直接持有 JWT**
- 通过 `SessionStateCheckFilter`（挂载于 `SecurityContextHolderFilter` 之后）对每次已认证请求触发 AT 懒刷新：
  - AT 未过期 → 取缓存，无网络开销
  - AT 过期 → 用 RT 向 SAS 刷新，SAS 侧重新验证用户状态
  - SAS 拒绝续发（`OAuth2AuthorizationException`）→ 强制使 Session 失效，返回 HTTP 401
- 处理登出：撤销 Token → 销毁 Session → 返回 `{"redirectUrl":"..."}`

**Customizer 链**（`OAuth2ClientSecurityCustomizer` SPI，按 `getOrder()` 注入）：

| Customizer | Order | 作用 |
|------------|-------|------|
| `OAuth2SecurityCustomizer`（oauth-biz） | 0 | 注册路径规则（见下）；收集所有 `AuthorizationManager` Bean 组成 `securityPolicies` 链 |
| `UpmsSecurityConfiguration`（upms-biz） | 100 | 仅提供 `UpmsGrantedAuthoritiesMapper`（OAuth2 登录回调时加载 ROLE_* 和资源权限） |
| `OAuthFormLoginCustomizer`（oauth-biz） | LOWEST | 配置 formLogin JSON handlers；`exceptionHandling`：`/api/**` → 401 JSON，其他路径 → 302 |

`OAuth2SecurityCustomizer` 注册的路径规则（首匹配，顺序固定）：

```
/api/oauth/user/profile → authenticated()         首匹配；pre_auth_reset 用户可访问
/api/oauth/user/**      → hasAuthority("SCOPE_upms")  仅服务账号可访问
/api/**                  → securityPolicies 链
    ├─ [Order   1] PasswordRestrictionAuthorizationManager（oauth-biz）
    │               含 SCOPE_pre_auth_reset → 403
    ├─ [Order 100] UpmsDynamicAuthorizationManager（upms-biz）
    │               ROLE_ADMIN → 放行；URL 不在 sys_resource → 403；否则校验权限串
    └─ fallback    AuthenticatedAuthorizationManager → 已认证则放行
```

---

### 1.3 Resource Server

**部署**：`snack-oauth2-resource-server-spring-boot-starter`（自动配置），挂载于 `snack-upms-server`。分布式模式下 `snack-oauth-server` 也用此 Starter 保护自身管理端点。

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

前端 SPA 自带登录页（`/user/login`），不依赖 SAS 的 Server-Side 渲染登录页。

```
前端 SPA (/user/login)        BFF (Order 2)                   SAS (Order 1)
  │                                │                                │
  │ 访问受保护路由                  │                                │
  │──────────────────────────────>│                                │
  │                                │ onPageChange: 无 currentUser  │
  │<── history.replace('/user/login?redirect=...')                 │
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

> ⚠️ **GAP-8**：`SessionStateCheckFilter` 返回的 `{"redirectUrl":"..."}` 与 `BizAuthenticationEntryPoint` 的标准格式 `{"code":"2003","data":{"loginUrl":"..."}}` 不一致。前端 `requestErrorConfig.ts` 当前只识别 `data?.data?.loginUrl`，无法处理 `redirectUrl` 键，导致会话中 AT 刷新失败时前端仅展示泛化错误 toast 而不自动跳转登录页。用户需等下次请求触发 `BizAuthenticationEntryPoint` 才能完成重定向。详见 §11 GAP-8。

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
  │                                    │ client_credentials → AT(SCOPE_upms)│
  │                                    │ POST /api/oauth/user              │
  │                                    │ Authorization: Bearer <AT>         │
  │                                    │───────────────────────────────────>│
  │                                    │        Resource Server (Order 3)   │
  │                                    │        JWT 验证 + hasAuthority("SCOPE_upms")
  │                                    │        OAuthUserAdminController    │
  │<───────────────────────────────────│<── 201 Created ────────────────────│
```

---

## 3. Token 设计

### 3.1 Token 类型与 TTL

| Token 类型 | TTL | 说明 |
|-----------|-----|------|
| Authorization Code | 5 分钟 | 一次性，换 Token 后失效 |
| Access Token | **5 分钟** | BFF 懒刷新，用户无感 |
| Refresh Token | **1 小时** | `reuseRefreshTokens=true`，不轮换 |
| 受限 AT（pre_auth_reset） | 5 分钟 | `OAuth2TokenCustomizerImpl` 强制覆盖 TTL |

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

**受限用户**（初始密码 / 密码过期）：
```json
{
  "sub": "username",
  "scope": ["openid", "pre_auth_reset"],
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
/api/oauth/user/profile → authenticated()
    首匹配；pre_auth_reset 用户也可访问（用于加载改密状态）

/api/oauth/user/**（其余） → hasAuthority("SCOPE_upms")
    仅服务账号 snack-upms-service（client_credentials，scope=upms）可访问
    普通用户 AT scope 仅含 openid/profile，SAS 不会颁发 SCOPE_upms，永远无法匹配

/api/**（其余） → securityPolicies 链（OrderedStream 注入）
    ├─ [Order 1]   PasswordRestrictionAuthorizationManager
    │               含 SCOPE_pre_auth_reset → AuthorizationDecision(false) → 403
    ├─ [Order 100] UpmsDynamicAuthorizationManager（单体模式）
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

两种情形触发路径相同，均通过 `UserDetailsServiceImpl.getAuthorities()` 检测：

| 触发条件 | 字段 |
|---------|------|
| 初始密码 | `oauth_user.initialPassword = 1` |
| 密码过期（登录前） | `now > lastPasswordResetTime + 90天`（`SecurityProperties.isCredentialsExpired()`） |

```
① SAS 颁发受限 AT：
   getAuthorities() 检测到需改密 → authorities=[SCOPE_pre_auth_reset]
   OAuth2TokenCustomizerImpl → scope={openid, pre_auth_reset}, authorities=[], TTL=5min

② 授权码回调 → BFF 建立 Session（持有受限 AT）

③ app.tsx getInitialState()
   GET /api/upms/users/info（skipErrorHandler: true）
   SysUserVO.initialPassword=1 或 SysUserVO.expired=1
   → changePasswordRequired=true

④ app.tsx onPageChange 路由守卫：
   changePasswordRequired=true → history.replace('/account/change-password')

⑤ 用户提交新密码 → PUT /api/upms/users/password
   PasswordRestrictionAuthorizationManager：/api/upms/users/password 走 securityPolicies
   （pre_auth_reset 用户无权访问 → 403）

   ⚠️ 注意：改密接口需对 pre_auth_reset scope 用户放行，当前由前端路由守卫保证
      用户只能在改密页操作；若需防御性保护，可在 SecurityPolicy 为该接口添加例外规则。

⑥ 前端：
   await changePassword(values.password)         // PUT /api/upms/users/password
                                                  // OAuthUserServiceImpl 只更新用户字段，不删 oauth_authorization
   const { redirectUrl } = await logout()         // RevokeTokenLogoutHandler 软撤销 AT/RT（记录保留）
                                                  // JsonLogoutSuccessHandler 返回 OIDC logout URL
   message.success('密码修改成功，请重新登录')      // 等 1.5 秒
   window.location.href = redirectUrl             // → §5.7 OIDC logout → 重走 §2.1 登录流程
```

**关键设计**：`credentialsExpired=false` 是有意为之——若设为 `true`，框架在认证阶段抛异常，用户无法获得任何 Token，也就无法调用改密接口。权限降级方案让用户持有受限 Token 只能访问 `/profile` 完成改密，是正确的设计选择。

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
         HTTP 401：{"redirectUrl":"/oauth2/authorization/snack-upms-server"}
    ⑤ 前端当前仅展示泛化 toast（见 GAP-8），下次请求经 BizAuthenticationEntryPoint
       返回标准格式 → 前端显示"登录状态已过期，请重新登录"，1.5秒后跳转
    ⑥ 重新登录 → 同 §5.2 流程（密码已过期）→ 路由守卫跳转改密页
```

**检测时延**：最长 5 分钟（AT TTL）。`reuseRefreshTokens=true` 已确认，无 RT rotation 竞争风险。

**闭环状态**：⚠️（功能闭环，但 GAP-8 导致第一次 401 无跳转，体验待优化）

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

| 操作 | 接口 | oauth_user 副作用 |
|------|------|-------------------|
| 禁用 / 启用 | `PUT /api/upms/users/{id}` `{status:0/1}` | `enabled=0/1`；禁用时吊销所有 session |
| 解锁 | `PATCH /api/upms/users/{id}/unlock` | `locked=0, lockCount=0, lockUntil=null` |
| 重置密码 | `POST /api/upms/users/{id}/reset-password` | 密码更新，`initialPassword=1`，吊销所有 session |
| 强制下线 | `DELETE /api/upms/users/{id}/tokens` | 按 `principal_name` 删除 `oauth_authorization`（RT 立即失效） |
| 设置到期日 | `PUT /api/upms/users/{id}` `{expireDate:"2026-12-31"}` | `expire_date=设定值`；登录/刷新 Token 时自动判定（§GAP-6） |

禁用 / 吊销 session 后：已颁发 AT 最长仍有 5 分钟有效期（RS 纯 JWT 验证）。重置密码后下次登录触发 §5.2 强制改密流程。

**闭环状态**：✅（GAP-3 已修复）

---

### 5.6 无权限访问（403）

```
情形 A：pre_auth_reset 用户访问非 /profile 接口
    PasswordRestrictionAuthorizationManager → 含 SCOPE_pre_auth_reset → 403

情形 B：普通用户访问无 RBAC 权限的接口（单体模式）
    PasswordRestrictionAuthorizationManager → 通过
    UpmsDynamicAuthorizationManager：
        ROLE_ADMIN → 直接放行
        URL 不在 sys_resource → 403（deny-by-default）
        URL 在 sys_resource 但无权限 → 403

情形 C：访问 oauth-server 管理端点（/api/oauth/user/**）
    hasAuthority("SCOPE_upms")：
        snack-upms-service AT（scope=upms）→ 放行
        普通用户 AT（scope=openid,profile）→ 403
```

**闭环状态**：✅（单体：`UpmsDynamicAuthorizationManager` deny-by-default；分布式：`SCOPE_upms` + 网络隔离双重防线，GAP-1 已修复）

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
    JsonLogoutSuccessHandler（分布式模式）
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

JsonLogoutSuccessHandler（单体模式）
    HTTP 200: {"redirectUrl": "/oauth2/authorization/snack-upms-server"}
    前端: window.location.href = redirectUrl → 重走授权码流程（SAS session 已由 BFF invalidate 清除）
```

**关键设计**：
- `RevokeTokenLogoutHandler` 软撤销（标记 invalidated，不删记录），保证 OIDC logout 能通过 `id_token_hint` 找到 authorization 记录
- 改密时 `OAuthUserServiceImpl.update()` **不再硬删** oauth_authorization，避免 `/connect/logout` 报 400
- 多设备踢人由管理员 `DELETE /api/upms/users/{id}/tokens` 接口负责

**管理员强制踢人**：见 §5.5 `DELETE /api/upms/users/{id}/tokens`。RT 吊销后最长仍有 5 分钟 AT 有效期窗口（RS 纯 JWT 验证），立即生效需引入 Token Introspection，优先级低。

**闭环状态**：✅

---

### 闭环状态汇总

| 场景 | 状态 | 说明 |
|------|------|------|
| 新用户创建 | ✅ | 双写，事务保障 |
| 初始密码强制改密 | ✅ | 权限降级 + 5min 受限 Token |
| 密码过期（登录前） | ✅ | 同初始密码改密流程 |
| 密码过期（会话中） | ✅ | `SessionStateCheckFilter` + SAS 重验，GAP-8 已修复 |
| 登录失败与锁定 | ✅ | 阶梯锁定，审计日志完整 |
| 无权限访问（403） | ✅ | 单体：deny-by-default；分布式：`SCOPE_upms` 双重防线，GAP-1 已修复 |
| 用户禁用 / 账号管控 | ✅ | 禁用/解锁/重置密码/强制下线，GAP-3 已修复 |
| 账号到期（日期自动过期） | ✅ | `expire_date` OR `expired=1`，登录/刷新时重验，GAP-6 已修复 |
| 权限调整实时生效 | ✅ | AT 层 ≤5min 延迟，UPMS 资源层立即生效 |
| 登出 / Token 撤销 | ✅ | 审计 + 撤销 + Session 销毁链完整 |

---

## 6. 数据库关键表

### 6.1 oauth_user（用户认证信息）

| 字段 | 类型 | 语义 | 正常值 |
|------|------|------|------|
| username | varchar | 主键，登录名 | — |
| password | varchar | DelegatingPasswordEncoder BCrypt 哈希 | — |
| enabled | tinyint(1) | **Status 语义** | 1=启用, 0=禁用 |
| locked | tinyint(1) | **YesNo 语义** | 0=正常, 1=锁定 |
| expired | tinyint(1) | **YesNo 语义** | 0=正常, 1=过期（手动强制） |
| initial_password | tinyint(1) | **YesNo 语义** | 0=否, 1=是 |
| lock_count | int | 锁定累计次数 | 0 |
| lock_until | datetime | 临时锁定截止；null+locked=1 = 永久 | null |
| last_password_reset_time | datetime | 用于 90 天过期计算 | — |
| expire_date | date | 账号到期日；null=永不到期；登录时与 `expired=1` 做 OR 判断 | null |

> ⚠️ `enabled` 使用 `Status` 枚举（ENABLED=1），其余布尔字段使用 `YesNo` 枚举（NO=0=正常）。语义不统一，赋值必须使用正确的枚举常量。

### 6.2 sys_user 关联体系

```
sys_user ──(username)──> sys_user_role ──(roleCode)──> sys_role
                                                              │(roleCode)
                                                        sys_role_resource
                                                              │(resourceId)
                                                        sys_resource
```

### 6.3 sys_login_log（登录审计）

| 字段 | 说明 |
|------|------|
| username | 登录用户名 |
| action | LOGIN_SUCCESS / LOGIN_FAILURE / LOGOUT |
| failure_reason | INVALID_CREDENTIALS / ACCOUNT_LOCKED / ACCOUNT_DISABLED / PASSWORD_EXPIRED |
| ip_address | 客户端 IP（支持 X-Forwarded-For） |
| user_agent | 浏览器 UA（最长 500 字符截断） |
| session_id | HTTP Session ID |

---

## 7. 部署模式

### 7.1 单体 vs 分布式

| 维度 | 分布式（正式目标） | 单体（当前开发） |
|------|-----------------|----------------|
| 进程数 | 2：snack-oauth-server + snack-upms-server | 1：snack-upms-server 同时承担 SAS + BFF + RS |
| `OAUTH_SERVER_URL` | 指向独立 oauth-server | 不设置，默认 `localhost:${server.port}` |
| `DefaultSecurityConfig` | 在 oauth-server 进程生效（无 Client Starter） | 不生效（Order 2 Client 链已存在） |
| OAuth2UserClient 调用 | 跨进程 HTTP | 进程内 loopback HTTP（仍需携带有效 Bearer Token） |

### 7.2 各进程安全链

**分布式**：
```
snack-oauth-server
├─ Order 1: AuthorizationServerSecurityFilterChain
└─ Order 2: DefaultSecurityConfig（form-login + /api/oauth/user/** 保护）

snack-upms-server
├─ Order 2: OAuth2ClientSecurityFilterChain
└─ Order 3: ResourceServerSecurityFilterChain
```

**单体**：
```
snack-upms-server
├─ Order 1: AuthorizationServerSecurityFilterChain
├─ Order 2: OAuth2ClientSecurityFilterChain（DefaultSecurityConfig 因 Bean 存在而不生效）
└─ Order 3: ResourceServerSecurityFilterChain
```

### 7.3 切换方式

**单体 → 分布式**：
1. 部署独立 `snack-oauth-server`（端口 9000）
2. 设置 `OAUTH_SERVER_URL=http://<oauth-server-host>:<port>`
3. 确认 `redirect_uris` 回调地址指向 upms-server
4. （可选）从 `snack-upms-server/build.gradle` 移除 `snack-oauth-biz` 依赖，减少不必要的 Bean 初始化

**分布式 → 单体**：取消设置 `OAUTH_SERVER_URL`，确保 `snack-upms-server` 引入 `snack-oauth-biz` 依赖。

> 切换的唯一必要操作是 `OAUTH_SERVER_URL`，不涉及代码改动。单体模式下 `snack-upms-server` 内的 SAS 端点在分布式时依然启动，但所有 OAuth2 流量走 `OAUTH_SERVER_URL` 指向的外部服务，本地 SAS 不会收到用户流量。

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
```

### UPMS 客户端（`application-upms.yml`）

```yaml
snack:
  oauth2:
    client:
      server-url: ${OAUTH_SERVER_URL:http://localhost:${server.port:8080}}
      login-page: http://localhost:8000/user/login     # SPA 登录页地址
      default-success-url: http://localhost:8000/      # 登录成功后默认跳转

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

### Token 配置（`oauth_registered_client.token_settings`）

```json
{
  "settings.token.access-token-time-to-live":  "PT5M",
  "settings.token.refresh-token-time-to-live": "PT1H",
  "settings.token.reuse-refresh-tokens":       true
}
```

---

## 9. HTTP 响应速查

### 响应格式

```json
{ "code": null,    "msg": "Success",   "data": {...} }
{ "code": "2003",  "msg": "...",        "data": null  }
{ "code": "2003",  "msg": "...",        "data": {"loginUrl": "..."} }
```

**错误码**：

| 错误码 | 含义 |
|--------|------|
| `null` | 成功 |
| `1000` | 系统内部错误 |
| `1001` | 外部接口调用异常 |
| `1002` | 请求参数无效 |
| `1003` | 资源不存在 |
| `2000` | 数据已存在 |
| `2001` | 数据不存在 |
| `2002` | 数据状态错误 |
| `2003` | 权限不足（未认证 / 无权限） |
| `2004` | 操作不允许 |

---

### A. 表单登录（POST /auth-api/login → 代理到 POST /login）

| 场景 | Status | 响应体 | 处理器 |
|------|--------|--------|--------|
| 登录成功 | **200** | `{"code":null,"data":{"redirectUrl":"<savedUrl或null>"}}` | `JsonAuthenticationSuccessHandler` |
| 失败（密码错误 / 锁定 / 禁用） | **401** | `{"code":"2003","msg":"<Spring i18n 消息>"}` | `JsonAuthenticationFailureHandler` |

> `redirectUrl` 来自 `HttpSessionRequestCache`（登录前被拦截的 OAuth2 授权请求 URL）。前端拿到后执行 `window.location.href = redirectUrl`，完成完整授权码流程；若为 `null` 则前端回退到安全校验后的 redirect 参数或根路径。

---

### B. OAuth2 授权码回调（GET /login/oauth2/code）

| 场景 | Status | 响应 | 处理器 |
|------|--------|------|--------|
| 授权成功 | **302** | 重定向至 `defaultSuccessUrl`（`http://localhost:8000/`） | `SimpleUrlAuthenticationSuccessHandler` |
| 授权失败 | **302** | 重定向至 `/login?error` | Spring 默认 |

---

### C. API 访问（Session Cookie）

**C-1. 已登录用户**

| 场景 | Status | 响应体 | 处理器 |
|------|--------|--------|--------|
| 有权限 | **200** | 业务数据 | Controller |
| 已认证但无 RBAC 权限 | **403** | `{"code":"2003","msg":"Access Denied"}` | `BizAccessDeniedHandler` |
| `pre_auth_reset` 用户访问非 profile 接口 | **403** | `{"code":"2003","msg":"Access Denied"}` | `PasswordRestrictionAuthorizationManager` |
| AT 未过期，会话中密码到期（5min 窗口内） | **200** | 业务数据（缓存 AT 有效） | Controller |
| AT 过期，SAS 刷新时检测到密码 / 账号异常 | **401** | `{"redirectUrl":"/oauth2/authorization/snack-upms-server"}` ⚠️ | `SessionStateCheckFilter` |

> ⚠️ `SessionStateCheckFilter` 的 401 响应格式（`{"redirectUrl":"..."}`）与 `BizAuthenticationEntryPoint` 的标准格式（`{"code":"2003","data":{"loginUrl":"..."}}`）不一致，前端当前无法自动跳转（见 GAP-8）。

**C-2. 未登录 / Session 超时**

| 请求类型 | Status | 响应体 | 处理器 |
|----------|--------|--------|--------|
| `/api/**` AJAX | **401** | `{"code":"2003","data":{"loginUrl":"/oauth2/authorization/snack-upms-server"}}` | `BizAuthenticationEntryPoint` |
| 浏览器直接导航 | **302** | — 重定向至 OAuth2 授权端点 | `LoginUrlAuthenticationEntryPoint` |

> 前端 `requestErrorConfig.ts` 收到 401 含 `data.loginUrl` 时展示"登录状态已过期，请重新登录"提示，1.5 秒后跳转。**前端不硬编码授权端点**，地址由后端响应动态提供。

---

### D. Bearer Token 访问（携带 Authorization: Bearer 头）

| 场景 | Status | 响应格式 | 处理器 |
|------|--------|---------|--------|
| Token 无效 / 过期 | **401** | Spring 原生：`{"error":"invalid_token","error_description":"..."}` | `BearerTokenAuthenticationEntryPoint` |
| Token 有效，RBAC 拒绝 | **403** | ApiResponse：`{"code":"2003","msg":"Access Denied"}` | `BizAccessDeniedHandler` |

> Bearer 请求的 Token 层 401 格式与 Session 请求不同（Spring 原生 vs ApiResponse），是现有不一致点之一。

---

### E. 登出（POST /auth-api/logout → 代理到 POST /logout）

| 场景 | Status | 响应体 | 处理器 |
|------|--------|--------|--------|
| 单体登出 | **200** | `{"redirectUrl": "/oauth2/authorization/snack-upms-server"}` | `JsonLogoutSuccessHandler` |
| 分布式登出 | **200** | `{"redirectUrl": "http://SAS/connect/logout?post_logout_redirect_uri=...&id_token_hint=..."}` | `JsonLogoutSuccessHandler` |

---

### F. 应用层异常（Controller 层，`@RestControllerAdvice`）

| 场景 | Status | `code` |
|------|--------|--------|
| 参数校验失败 | **400** | `"1002"` |
| 路径不存在 | **404** | `"1003"` |
| 业务异常（`BusinessException`） | **500** ⚠️ | 异常携带的错误码 |
| 系统错误 | **500** | `"1000"` |

> ⚠️ `BusinessException` 返回 HTTP 500 语义不准，建议改为 4xx。

---

## 10. 关键源码位置

| 组件 | 文件路径 | 关键方法 |
|------|---------|---------|
| **SAS** | | |
| SAS 安全链配置 | `snack-oauth-biz/.../security/config/AuthorizationServerConfig.java` | `authorizationServerSecurityFilterChain()` |
| 默认安全配置（分布式） | `snack-oauth-biz/.../security/config/DefaultSecurityConfig.java` | `defaultSecurityFilterChain()` |
| JWT Token 定制器 | `snack-oauth-biz/.../security/config/OAuth2TokenCustomizerImpl.java` | `customize()`（权限降级 + refresh_token 重验用户状态） |
| UserDetailsService | `snack-oauth-biz/.../service/impl/UserDetailsServiceImpl.java` | `loadUserByUsername()`, `getAuthorities()`（含时间过期检测） |
| 缓存锁定检查 | `snack-oauth-biz/.../security/LockCheckingUserDetailsService.java` | `loadUserByUsername()` |
| 认证事件（锁定） | `snack-oauth-biz/.../security/OAuth2AuthenticationEvents.java` | `onAuthenticationFailure/Success()` |
| 安全策略配置 | `snack-oauth-biz/.../security/config/SecurityProperties.java` | `isCredentialsExpired()` |
| OAuth2 用户服务 | `snack-oauth-biz/.../service/impl/OAuthUserServiceImpl.java` | `create()`, `update()` |
| Profile 控制器 | `snack-oauth-biz/.../controller/OAuthUserProfileController.java` | `getProfile()` (`GET /api/oauth/user/profile`) |
| **OAuth2 Client / BFF** | | |
| Client 自动配置 | `snack-oauth2-client-starter/.../config/OAuth2ClientAutoConfiguration.java` | `oauth2ClientSecurityFilterChain()` |
| AT 懒刷新过滤器 | `snack-oauth2-client-starter/.../security/SessionStateCheckFilter.java` | `doFilterInternal()`（AT 刷新 + 异常时强制登出 401） |
| Token 撤销登出处理器 | `snack-oauth2-client-starter/.../security/RevokeTokenLogoutHandler.java` | `logout()` |
| 审计登出处理器 | `snack-oauth2-client-starter/.../security/AuditLogoutHandler.java` | `logout()` |
| 登出成功处理器 | `snack-oauth2-client-starter/.../security/JsonLogoutSuccessHandler.java` | `onLogoutSuccess()` |
| OAuth2 安全规则 | `snack-oauth-biz/.../security/OAuth2SecurityCustomizer.java` | `configureAuthorization()`（Order 0） |
| 表单登录注入 | `snack-oauth-biz/.../security/config/OAuthFormLoginCustomizer.java` | `customize(http)`（路径分派 EntryPoint） |
| 登录成功处理器 | `snack-oauth-biz/.../security/handler/JsonAuthenticationSuccessHandler.java` | `onAuthenticationSuccess()`（返回 redirectUrl JSON） |
| 登录失败处理器 | `snack-oauth-biz/.../security/handler/JsonAuthenticationFailureHandler.java` | `onAuthenticationFailure()`（返回 401 JSON） |
| **Resource Server** | | |
| RS 自动配置 | `snack-oauth2-resource-server-starter/.../ResourceServerAutoConfiguration.java` | — |
| 密码强制改密管理器 | `snack-oauth-biz/.../security/PasswordRestrictionAuthorizationManager.java` | `authorize()` |
| RBAC 授权管理器 | `snack-upms-biz/.../security/UpmsDynamicAuthorizationManager.java` | `authorize()`（Order 100，deny-by-default） |
| UPMS 权限映射器 | `snack-upms-biz/.../security/UpmsGrantedAuthoritiesMapper.java` | `mapAuthorities()`（OAuth2 回调时加载权限） |
| 401 处理器 | `snack-oauth-biz/.../security/handler/BizAuthenticationEntryPoint.java` | `commence()`（`/api/**` 未认证返回 JSON） |
| 403 处理器 | `snack-oauth-biz/.../security/handler/BizAccessDeniedHandler.java` | `handle()` |
| **UPMS 业务层** | | |
| UPMS 用户服务 | `snack-upms-biz/.../service/impl/SysUserServiceImpl.java` | `create()`, `update()` |
| UPMS 安全 Customizer | `snack-upms-biz/.../security/config/UpmsSecurityConfiguration.java` | `authoritiesMapper()` |
| UPMS 审计事件 | `snack-upms-biz/.../security/UpmsAuthenticationEvents.java` | `onSuccess()`, `onFailure()` |
| **前端** | | |
| 初始化状态 & 路由守卫 | `src/app.tsx` | `getInitialState()`, `onPageChange`（改密拦截） |
| 错误处理 | `src/requestErrorConfig.ts` | `errorHandler`（BizError skip / 非业务全局处理） |
| 认证服务 | `src/services/auth/index.ts` | `login()`, `logout()`, `getSysUserInfo()`, `changePassword()` |
| 登录页 | `src/pages/account/login/index.tsx` | — |
| 强制改密页 | `src/pages/account/change-password/index.tsx` | — |
| 账户设置页 | `src/pages/account/settings/index.tsx` | — |

---

## 11. 已知缺口

### ~~BUG-1~~：`expired` 字段枚举语义混用 ✅ 已修复

`expired` 与 `locked` 语义对称（0=正常，1=异常），但代码曾用 `Status.DISABLED=0` 做判断导致逻辑相反。

| 位置 | 修复前 | 修复后 |
|------|--------|--------|
| `UserDetailsServiceImpl.loadUserByUsername()` | `accountExpired(equals(expired, Status.DISABLED=0))` | `accountExpired(equals(expired, YesNoStatus.YES=1))` |
| `OAuthUserServiceImpl.create()` | `setExpired(Status.ENABLED=1)` | `setExpired(YesNoStatus.NO=0)` |
| `OAuthUserServiceImpl.update()` | `setExpired(Status.ENABLED=1)` | `setExpired(YesNoStatus.NO=0)` |

---

### ~~GAP-1~~：分布式模式 oauth-server 管理端点防护不足 ✅ 已修复

1. ✅ 注册服务账号 `snack-upms-service`（client_credentials，scope=upms）
2. ✅ `OAuth2UserClientConfiguration` 改用 `OAuth2ClientHttpRequestInterceptor`，固定使用 `snack-upms-service` AT，与用户 AT 完全解耦
3. ✅ `OAuth2SecurityPolicy` 收紧：`/api/oauth/user/**` 改为 `hasAuthority("SCOPE_upms")`

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

### GAP-4：Refresh Token 轮换（BFF 架构下低优先级）

BFF 模式下 RT 由服务端持有，实际泄露路径是 DB 入侵而非前端被盗，`reuse-refresh-tokens=true` 对当前架构影响有限。若未来增加非 BFF 客户端（移动端等），应启用 RT 轮换。

---

### GAP-5：密码复杂度无后端校验

`PUT /api/upms/users/password` 接受任意字符串。建议在 `SecurityProperties` 增加复杂度配置（最小长度、大小写/特殊字符要求），在 `SysUserServiceImpl` 或 `OAuthUserServiceImpl` 中校验。前端已有正则校验，但后端缺乏防御。

---

### ~~GAP-6~~：账号自动到期机制缺失 ✅ 已修复

1. ✅ `oauth_user` 增加 `expire_date date` 字段（null=永不到期）
2. ✅ `UserDetailsServiceImpl.loadUserByUsername()` 的 `accountExpired` 改为 OR 逻辑：`expired=1` OR `expireDate < today`；无需定时任务，登录时及 refresh_token 重验时自然感知
3. ✅ `OAuth2UserDTO` / `SysUserDTO` 增加 `expireDate LocalDate`，管理员通过 `PUT /api/upms/users/{id}` 设置并透传至 oauth_user

---

### GAP-7：可空字段无法通过更新接口清除

后端配置 `spring.jackson.default-property-inclusion = NON_NULL`，响应中 null 字段不输出；前端构建请求时同样不携带 null 字段。因此 `null` 在当前栈中无法作为"主动清除"的信号传递给后端。

受影响字段（选填，业务上存在清除场景）：

| 字段 | 所在表 | 清除含义 |
|------|--------|---------|
| `expire_date` | oauth_user | 合同延期，取消到期限制 |
| `mobile` | oauth_user / sys_user | 解绑手机号（按业务决定是否支持） |
| `email` | oauth_user / sys_user | 解绑邮箱（按业务决定是否支持） |

**候选方案**：

- **方案 A（`clearFields` 列表）**：在 DTO 加 `Set<String> clearFields`，前端发 `{"clearFields":["expireDate"]}` 显式声明清除意图，后端按此列表重置字段为 null。改动最小，与现有 PUT 接口兼容。
- **方案 B（专用端点）**：`DELETE /users/{id}/expire-date` 等，语义清晰但前端需维护多个调用路径。
- **方案 C（JSON Merge Patch）**：改用 `PATCH` + RFC 7396，`null` 显式表示删除字段。需引入额外依赖并改变 HTTP 方法，适合字段清除场景较多时统一采用。

**当前状态**：待决策。`expire_date` 的清除需求最确定（合同延期场景），建议优先处理。

---

### ~~GAP-8~~：SessionStateCheckFilter 401 响应格式与分布式登出问题 ✅ 已修复

本 GAP 涵盖两个关联修复：

**修复 1 — SessionStateCheckFilter 响应格式**
`SessionStateCheckFilter` 响应改为包含 `data.loginUrl` 的标准结构，前端 `requestErrorConfig.ts` 能正确读取并跳转：
```json
{ "data": { "loginUrl": "/oauth2/authorization/snack-upms-server" } }
```

**修复 2 — 分布式模式改密后登出（OIDC RP-Initiated Logout）**

原问题链：改密 → `OAuthUserServiceImpl` 硬删 `oauth_authorization` → BFF 调 `/connect/logout` → Spring AS 找不到 authorization → 400。

完整修复：
1. `OAuth2TokenCustomizerImpl`：受限 Token scope 保留 `openid`（`["openid","pre_auth_reset"]`），ID Token 符合 OIDC 规范，`/connect/logout` 的 `id_token_hint` 校验通过
2. `OAuthUserServiceImpl.update()`：改密时**不再硬删** oauth_authorization，由 BFF 的 `RevokeTokenLogoutHandler` 软撤销（标记 invalidated，记录保留）
3. `JsonLogoutSuccessHandler`：分布式模式下构造 OIDC 标准登出 URL（`/connect/logout?post_logout_redirect_uri=...&id_token_hint=...`）
4. `oauth_registered_client.post_logout_redirect_uris`：注册允许的登出回调 URI（`http://localhost:8080/oauth2/authorization/snack-upms-server`），Spring AS 7.x 强制校验此字段
