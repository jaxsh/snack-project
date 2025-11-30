# Snack Project

一个基于 **Spring Boot 4.0.0** 和 **Java 17** 的企业级微服务基础框架，提供开箱即用的通用功能模块。

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE.txt)

---

## 📦 项目概览

Snack Project 是一个**模块化、可插拔**的企业级基础框架，旨在简化 Spring Boot 应用的开发。通过提供一系列开箱即用的 Starter 模块，让开发者专注于业务逻辑而非基础设施。

### 核心特性

- ✅ **模块化设计**：业务与基础设施完全分离
- ✅ **开箱即用**：提供常用功能的 Spring Boot Starter
- ✅ **高度可配置**：支持灵活的配置和自定义扩展
- ✅ **类型安全**：完整的 TypeScript 类型定义
- ✅ **IDE 友好**：配置属性自动提示
- ✅ **生产就绪**：内置审计、日志追踪等企业级功能

---

## 🏗️ 项目结构

```
snack-project/
├── snack-framework/              # 基础框架模块
│   ├── snack-mybatisplus-spring-boot-starter/  # MyBatis-Plus 增强
│   ├── snack-web-spring-boot-starter/          # Web 通用功能
│   ├── snack-mdc-spring-boot-starter/          # MDC 日志追踪
│   ├── snack-redis-spring-boot-starter/        # Redis 增强
│   └── snack-ldap-spring-boot-starter/         # LDAP 集成
├── snack-module/                 # 业务模块
│   └── snack-upms-server/                      # 用户权限管理系统
├── buildSrc/                     # Gradle 构建插件
├── config/                       # Checkstyle & PMD 配置
└── dependencies/                 # 依赖版本管理
```

---

## 🎯 框架模块

### 1. MyBatis-Plus Starter

**GraphQL 风格的通用查询 API**，让前端灵活构建复杂查询条件。

#### 核心功能

- ✅ **19 种查询操作符**：比较、模糊、集合、空值、区间、逻辑
- ✅ **无限嵌套逻辑**：支持 `_and`、`_or`、`_not` 组合
- ✅ **字段选择与排序**：类似 GraphQL 的 `select` 和 `orderBy`
- ✅ **审计功能**：自动填充创建人、修改人、时间戳
- ✅ **用户上下文**：集成 Spring Security，自动注入当前用户

#### 快速示例

**前端查询**：
```json
{
  "select": ["id", "username", "email"],
  "where": {
    "_and": [
      { "username": { "_like": "admin" } },
      { "age": { "_between": [18, 30] } }
    ]
  },
  "orderBy": [
    { "field": "createTime", "direction": "desc" }
  ]
}
```

**生成 SQL**：
```sql
SELECT id, username, email
FROM user
WHERE username LIKE '%admin%' AND age BETWEEN 18 AND 30
ORDER BY create_time DESC
```

> 📚 **详细文档**：[snack-mybatisplus-spring-boot-starter/README.md](snack-framework/snack-mybatisplus-spring-boot-starter/README.md)

---

### 2. Web Starter

**通用 Web 功能增强**，提供统一的响应处理和异常管理。

#### 核心功能

- ✅ **全局响应包装**：自动包装 Controller 返回值
- ✅ **全局异常处理**：优雅处理业务异常和系统异常
- ✅ **RestClient 支持**：增强的 HTTP 客户端
- ✅ **请求验证**：集成 Jakarta Validation
- ✅ **Logbook 集成**（可选）：HTTP 请求日志

#### 配置示例

```yaml
spring:
  mvc:
    validation:
      fail-fast: true  # 快速失败模式
```

---

### 3. MDC Starter

**全链路日志追踪**，自动生成和传播 Trace ID。

#### 核心功能

- ✅ **自动 Trace ID 生成**：UUID 或自定义生成器
- ✅ **Logback 动态注入**：自动修改日志格式
- ✅ **异步任务支持**：`@Async` 自动传播 MDC
- ✅ **HTTP 拦截器**：请求开始生成，结束清理
- ✅ **响应头注入**：可选地将 Trace ID 返回给前端

#### 配置示例

```yaml
logging:
  mdc:
    enabled: true
    trace-id-key: traceId
    trace-id-pattern: "[%X{traceId:-}] "
    include-in-response: true
    response-header-name: X-Trace-Id
```

**日志输出**：
```
2025-11-30 10:30:45.123 INFO [a1b2c3d4-e5f6-7890-abcd-ef1234567890] User login successful
```

---

### 4. Redis Starter

**Redis 功能增强**，简化 Redis 操作。

#### 核心功能

- ✅ 基于 `spring-boot-starter-data-redis`
- ✅ 提供开箱即用的 Redis 配置
- ✅ 支持常见数据结构操作

---

### 5. LDAP Starter

**LDAP 集成**，简化企业 LDAP 认证。

#### 核心功能

- ✅ 基于 `spring-boot-starter-data-ldap`
- ✅ 支持 LDAP 用户认证
- ✅ 企业级目录服务集成

---

## 🚀 快速开始

### 环境要求

- **JDK**: 21（编译），目标字节码 Java 17
- **Gradle**: 9.2.1+
- **Spring Boot**: 4.0.0

### 1. 克隆项目

```bash
git clone https://github.com/your-org/snack-project.git
cd snack-project
```

### 2. 构建项目

```bash
./gradlew build
```

### 3. 在您的项目中使用

#### 添加依赖 BOM

```gradle
dependencies {
    // 引入 Snack Framework BOM
    implementation platform(project(':snack-project-dependencies'))

    // 选择需要的模块
    implementation 'org.jax.snack.framework:snack-mybatisplus-spring-boot-starter'
    implementation 'org.jax.snack.framework:snack-web-spring-boot-starter'
    implementation 'org.jax.snack.framework:snack-mdc-spring-boot-starter'
}
```

#### Spring Boot 主类

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

所有功能**自动装配**，无需额外配置！

---

## 📚 技术栈

### 核心框架

| 技术                  | 版本     | 说明     |
|---------------------|--------|--------|
| **Spring Boot**     | 4.0.0  | 应用框架   |
| **Spring Security** | 7.0.x  | 安全框架   |
| **MyBatis-Plus**    | 3.5.14 | ORM 增强 |
| **Logback**         | 1.5.21 | 日志框架   |

### 工具库

| 技术                  | 版本     | 说明            |
|---------------------|--------|---------------|
| **Lombok**          | 自动管理   | 减少样板代码        |
| **Zalando Logbook** | 3.12.3 | HTTP 请求日志（可选） |
| **AssertJ**         | 3.27.6 | 流式断言          |
| **Mockito**         | 5.20.0 | Mock 框架       |

### 构建工具

- **Gradle**: 9.2.1 (Groovy DSL)
- **Java Format**: Spring Java Format
- **Quality**: PMD + Checkstyle

---

## 🛠️ 构建系统

### Convention Plugins

项目使用 **Convention over Configuration** 原则：

```gradle
// 普通 Java 模块
plugins {
    id 'java-library'
}

// Spring Boot 库模块
plugins {
    id 'java-library'
    id 'spring-boot-library-conventions'
}

// Spring Boot 应用模块
plugins {
    id 'java-application'
    id 'spring-boot-application-conventions'
}
```

### 版本管理

所有依赖版本在 `gradle/libs.versions.toml` 中统一管理：

```toml
[versions]
org-springframework-boot = '4.0.0'
com-baomidou-mybatis-plus = '3.5.14'

[libraries]
spring-boot-dependencies = { module = 'org.springframework.boot:spring-boot-dependencies', version.ref = 'org-springframework-boot' }
com-baomidou-mybatis-plus-bom = { module = 'com.baomidou:mybatis-plus-bom', version.ref = 'com-baomidou-mybatis-plus' }
```

---

## 🧪 测试

### 运行所有测试

```bash
./gradlew test
```

### 运行特定模块测试

```bash
./gradlew :snack-framework:snack-mybatisplus-spring-boot-starter:test
```

---

## 📦 模块发布

### 构建 JAR

```bash
./gradlew :snack-framework:snack-mybatisplus-spring-boot-starter:jar
```

### 发布到 Maven 本地

```bash
./gradlew publishToMavenLocal
```

---

## 🤝 贡献指南

### Commit Message 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```text
<type>(<scope>): <subject>

<body>

<footer>
```

---

## 📄 License

本项目遵循 [Apache License 2.0](LICENSE.txt) 开源协议。

---

## 👥 作者

**Jax Jiang**

- Email: jiang.tao.sh.cn@gmail.com
- GitHub: [@jaxsh](https://github.com/jaxsh)

---

## 🔗 相关链接

- [Spring Boot 文档](https://docs.spring.io/spring-boot/4.0.x/reference/)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Gradle 文档](https://docs.gradle.org/9.2.1/)

---

## 📝 更新日志

### v1.0.0 (2025-11-30)

**新增功能**:
- ✅ MyBatis-Plus Starter：GraphQL 风格查询 API
- ✅ Web Starter：全局响应包装和异常处理
- ✅ MDC Starter：全链路日志追踪
- ✅ Redis Starter：Redis 功能增强
- ✅ LDAP Starter：LDAP 集成

**技术栈**:
- Spring Boot 4.0.0
- Java 17 (目标) / JDK 21 (编译)
- MyBatis-Plus 3.5.14
