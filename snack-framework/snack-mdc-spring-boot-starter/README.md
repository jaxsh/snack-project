# Snack Framework - MDC Auto-Configuration

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-17+-green.svg)](https://www.oracle.com/java/technologies/downloads/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

一个为 Spring Boot 应用提供全自动、零侵入的 MDC（Mapped Diagnostic Context）链路追踪 ID 解决方案。

该模块可以自动为 Web 请求、异步任务 (`@Async`)、定时任务 (`@Scheduled`) 以及 `CompletableFuture` 注入和传播一个唯一的 `traceId`，并将其无缝集成到 Logback 日志输出中，极大地简化了分布式系统中的日志追踪和问题排查。

## ✨ 核心功能

-   **全自动 TraceId 管理**：自动为每个请求或任务生命周期生成和清理 `traceId`。
-   **无缝 Logback 集成**：在应用启动时，自动将 `traceId` 注入到您的 Logback 日志格式中，无需手动修改 `logback.xml`。
-   **全面的上下文传播**：
    -   **Web 请求**：通过 `HandlerInterceptor` 拦截 HTTP 请求，支持从请求头中获取 `traceId`，或生成新的 `traceId` 并添加到响应头中。
    -   **异步任务 (`@Async`)**：通过 `TaskDecorator` 自动将父线程的 `traceId` 传播到 `@Async` 任务中。
    -   **定时任务 (`@Scheduled`)**：通过 AOP 代理自动为每个定时任务的执行生成一个新的 `traceId`。
    -   **CompletableFuture**：提供一个支持 MDC 传播的 `@Primary` `Executor` Bean，解决 `CompletableFuture` 异步执行时的 `traceId` 丢失问题。
-   **高度可配置**：通过 `application.yml` 或 `application.properties` 提供丰富的配置选项，可轻松启用/禁用、自定义 `traceId` 键名、日志注入位置和格式。
-   **灵活可扩展**：允许通过实现 `TraceIdGenerator` 接口来提供自定义的 `traceId` 生成策略。

## 🚀 快速开始

### 1. 添加依赖

将此模块作为依赖项添加到您的项目中。（注：当前项目尚未发布到公共仓库，以下为示例）

**Maven:**

```xml
<dependency>
    <groupId>org.jax.snack.framework</groupId>
    <artifactId>snack-mdc-spring-boot-starter</artifactId>
    <version>1.0.0</version> <!-- 请替换为实际版本 -->
</dependency>
```

**Gradle:**

```groovy
implementation 'org.jax.snack.framework:snack-mdc-spring-boot-starter:1.0.0' // 请替换为实际版本
```

### 2. 享受自动化

完成！无需任何额外代码。启动您的 Spring Boot 应用，`traceId` 将会自动出现在您的日志中。

默认情况下，`traceId` 会被注入到日志格式中线程名 (`%thread`) 的后面。

**日志输出示例:**

```
2024-06-16T18:12:09.982+08:00  INFO 15976 --- [nio-8080-exec-1] [27f1ccfc8c984a7f] c.e.m.controller.OrderController         : Received a new order request.
```

## 🔧 配置指南

该模块可以通过 `application.properties` 或 `application.yml`进行详细配置。所有配置项都以 `logging.mdc` 为前缀。

以下是一个完整的 `application.yml` 配置示例：

```yaml
logging:
  mdc:
    # 是否启用MDC traceId功能 (默认: true)
    enabled: true

    # traceId在MDC中的键名，用于在日志格式中引用 (默认: traceId)
    trace-id-key: "traceId"

    # 定义将要注入到日志格式中的 traceId 内容模板.
    # 占位符 {traceIdKey} 会被上面的 trace-id-key 值替换.
    # (默认: "[%X{{traceIdKey}:-}] ")
    trace-id-pattern: "|TID:%X{{traceIdKey}:-}| "

    # 是否在HTTP响应头中包含traceId (默认: true)
    include-in-response: true

    # 响应头中traceId的键名 (默认: X-Trace-Id)
    response-header-name: "X-Trace-Id"

    # 指定traceId注入位置的目标Logback转换器。
    # 默认注入到线程名 (%thread) 之后。可改为其他转换器，如日志级别 (%level)。
    # (默认: ch.qos.logback.classic.pattern.ThreadConverter)
    target-converter: ch.qos.logback.classic.pattern.LevelConverter

    # 需要包含traceId处理的URL路径模式 (默认: "/**")
    include-patterns:
      - /api/**
      - /users/**

    # 需要排除traceId处理的URL路径模式 (默认: /health, /actuator/**, /favicon.ico)
    exclude-patterns:
      - /api/public/**
      - /swagger-ui/**
      - /v3/api-docs/**
```

### 配置项详解

| 属性名                  | 描述                                                                                                                                                             | 默认值                                             |
| ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------- |
| `enabled`               | 主开关，控制是否启用整个 MDC `traceId` 功能。                                                                                                                    | `true`                                             |
| `trace-id-key`          | 定义在 MDC 上下文中存储 `traceId` 的键。日志格式中可以通过 `%X{...}` 来引用。                                                                                      | `traceId`                                          |
| `trace-id-pattern`      | 定义 `traceId` 在日志中的显示格式模板。`{traceIdKey}` 会被自动替换为 `trace-id-key` 的值。                                                                       | `[%X{{traceIdKey}:-}] `                             |
| `target-converter`      | Logback 中用于定位注入点的转换器类。`traceId` 会被注入到此转换器之后。例如，`LevelConverter` 对应 `%level` 或 `%le`。如果找不到，会回退到在日志行首添加。           | `ch.qos.logback.classic.pattern.ThreadConverter`   |
| `include-in-response`   | 如果为 `true`，`traceId` 将被添加到每个 HTTP 响应的头中。                                                                                                          | `true`                                             |
| `response-header-name`  | 定义在 HTTP 请求/响应头中传递 `traceId` 的名称。                                                                                                                   | `X-Trace-Id`                                       |
| `include-patterns`      | Ant 风格的 URL 模式数组，只有匹配的路径才会被 MDC 拦截器处理。                                                                                                   | `["/**"]`                                          |
| `exclude-patterns`      | Ant 风格的 URL 模式数组，匹配的路径将**不会**被 MDC 拦截器处理。                                                                                                 | `["/health", "/actuator/**", "/favicon.ico"]`      |

## 🔬 高级用法

### 自定义 Trace ID 生成策略

默认使用 `UUID` 的前 16 位作为 `traceId`。如果您需要自定义生成逻辑（例如，集成 [Snowflake](https://en.wikipedia.org/wiki/Snowflake_ID) 算法），只需实现 `TraceIdGenerator` 接口并将其注册为 Spring Bean。

```java
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.springframework.stereotype.Component;

@Component
public class CustomTraceIdGenerator implements TraceIdGenerator {

    @Override
    public String generate() {
        // 在这里实现您自己的ID生成逻辑
        // 例如：return MySnowflakeIdGenerator.nextId();
        return "custom-" + System.currentTimeMillis();
    }
}
```

由于自动配置中使用了 `@ConditionalOnMissingBean`，您的自定义 Bean 将会自动替换默认的 `UuidTraceIdGenerator`。

## 💡 实现原理

-   **日志注入 (`MdcLogbackConfigurer`)**: 监听 Spring 的 `ApplicationReadyEvent` 事件，在应用启动后，以编程方式解析所有 Logback `Appender` 的日志格式。它会找到配置的 `target-converter` 对应的节点，并将 `trace-id-pattern` 渲染后的格式化字符串注入到节点树中，最后重新构建并应用新的日志格式。

-   **Web 请求 (`MdcInterceptor`)**: 作为一个标准的 `WebMvcConfigurer` 拦截器，在 `preHandle` 方法中从请求头获取 `traceId` 或生成一个新的，并放入 `MDC`。在 `afterCompletion` 方法中，确保 `MDC.clear()` 被调用，以防内存泄漏和线程污染。

-   **`@Async` 任务 (`MdcTaskDecorator`)**: 实现了 Spring 的 `TaskDecorator` 接口。在任务提交时，它会捕获当前线程的 MDC 上下文 (`MDC.getCopyOfContextMap()`)，然后在任务执行时，将捕获的上下文恢复到执行线程中。

-   **`@Scheduled` 任务 (`MdcSchedulingConfigurer`)**: 作为一个 `BeanPostProcessor`，它会识别 Spring 默认的 `ThreadPoolTaskScheduler` Bean，并使用 AOP 代理对其进行包装。代理会拦截所有任务提交方法（如 `schedule`、`submit`），在原始任务执行前，先生成一个新的 `traceId` 并放入 `MDC`，任务结束后再清理。

-   **`CompletableFuture` (`MdcAwareExecutor`)**: 定义了一个 `Executor` 装饰器，它包装了 Spring Boot 默认的 `applicationTaskExecutor`。这个装饰器被声明为 `@Primary`，因此当您在代码中注入 `Executor` 时（例如，用于 `CompletableFuture.supplyAsync(..., executor)`），会默认使用这个支持 MDC 传播的版本。

## 📜 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) 许可证。
