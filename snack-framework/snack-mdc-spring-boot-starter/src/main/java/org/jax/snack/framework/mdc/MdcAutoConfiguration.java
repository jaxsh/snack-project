/*
 * Copyright 2023-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jax.snack.framework.mdc;

import java.util.concurrent.Executor;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.jax.snack.framework.mdc.generator.UuidTraceIdGenerator;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MDC (Mapped Diagnostic Context) 自动配置类.
 * <p>
 * 该配置类负责初始化 Snack Framework 中与全链路追踪（Trace ID）相关的所有组件. 功能包括：
 * <ul>
 * <li>自动配置 {@link MdcLogbackConfigurer} 以支持 Logback 日志格式的动态注入.</li>
 * <li>自动配置 {@link MdcTaskDecorator} 以支持 {@code @Async} 异步任务的上下文传播.</li>
 * <li>提供默认的 {@link TraceIdGenerator} 实现（UUID）.</li>
 * <li>在 Web 环境下，注册 {@link MdcInterceptor} 以处理 HTTP 请求的 Trace ID.</li>
 * </ul>
 *
 * @author Jax Jiang
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "logging.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MdcProperties.class)
@org.springframework.boot.autoconfigure.AutoConfigureAfter(TaskExecutionAutoConfiguration.class)
public class MdcAutoConfiguration {

	private final MdcProperties properties;

	/**
	 * 配置 Logback 日志修改器.
	 * <p>
	 * 用于在应用启动时，动态修改 Logback 的 Appender 配置，将 Trace ID 格式注入到日志模式中.
	 * @return {@link MdcLogbackConfigurer} 实例
	 */
	@Bean
	public MdcLogbackConfigurer logbackConfigurer() {
		return new MdcLogbackConfigurer(this.properties);
	}

	/**
	 * 配置异步任务装饰器.
	 * <p>
	 * 确保使用 {@code @Async} 注解执行的异步任务能够继承父线程的 MDC 上下文（Trace ID） .
	 * @return {@link MdcTaskDecorator} 实例
	 */
	@Bean
	public MdcTaskDecorator mdcTaskDecorator() {
		return new MdcTaskDecorator();
	}

	/**
	 * 配置默认的 Trace ID 生成器.
	 * <p>
	 * 当容器中不存在 {@link TraceIdGenerator} 类型的 Bean 时，使用默认的 {@link UuidTraceIdGenerator}.
	 * 开发者可以通过自定义 Bean 来替换此默认实现.
	 * @return 默认的 {@link TraceIdGenerator} 实现
	 */
	@Bean
	@ConditionalOnMissingBean
	public TraceIdGenerator traceIdGenerator() {
		return new UuidTraceIdGenerator();
	}

	/**
	 * Web 环境专属配置.
	 * <p>
	 * 仅在 Servlet Web 应用环境下生效，用于配置 Spring MVC 拦截器.
	 *
	 * @author Jax Jiang
	 */
	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@RequiredArgsConstructor
	public static class MdcWebConfiguration implements WebMvcConfigurer {

		private final MdcProperties properties;

		private final TraceIdGenerator traceIdGenerator;

		/**
		 * 配置 MDC 拦截器.
		 * <p>
		 * 负责在 HTTP 请求开始时生成或提取 Trace ID 并放入 MDC，请求结束时清理 MDC.
		 * @return {@link MdcInterceptor} 实例
		 */
		@Bean
		public MdcInterceptor mdcInterceptor() {
			return new MdcInterceptor(this.properties, this.traceIdGenerator);
		}

		/**
		 * 注册拦截器到 Spring MVC.
		 * <p>
		 * 根据 {@link MdcProperties} 中的配置，应用包含和排除路径规则.
		 * @param registry 拦截器注册表
		 */
		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(this.mdcInterceptor())
				.addPathPatterns(this.properties.getIncludePatterns())
				.excludePathPatterns(this.properties.getExcludePatterns());
		}

	}

	/**
	 * CompletableFuture 线程池增强配置.
	 * <p>
	 * 当 Classpath 中存在 {@link TaskExecutor} 时生效. 旨在提供一个支持 MDC 传递的 {@link Executor}，解决
	 * {@code CompletableFuture} 等原生异步编程场景下 Trace ID 丢失的问题.
	 *
	 * @author Jax Jiang
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(TaskExecutor.class)
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
			name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
	public static class MdcCompletableFutureExecutorConfiguration {

		/**
		 * MDC 增强版 Executor 的 Bean 名称.
		 */
		public static final String MDC_AWARE_EXECUTOR_BEAN_NAME = "mdcAwareCompletableFutureExecutor";

		/**
		 * 配置 Executor.
		 * <p>
		 * <b>核心特性：</b>
		 * <ul>
		 * <li><b>首选 Bean（@Primary）：</b> 标记为 {@code @Primary}，使得直接注入 {@code Executor}
		 * 时默认使用此增强实例.</li>
		 * <li><b>包装机制：</b> 内部包装了 Spring Boot 自动配置的
		 * {@code applicationTaskExecutor}，复用其线程池参数配置.</li>
		 * </ul>
		 * <p>
		 * <b>注意：</b> 此 Bean 不会影响 {@code @Async} 的执行器选择（后者按名称匹配).
		 * @param applicationTaskExecutor Spring Boot 自动配置的任务执行器
		 * @return 包装后的 Executor 实例
		 */
		@Bean(name = MDC_AWARE_EXECUTOR_BEAN_NAME)
		@ConditionalOnMissingBean(name = MDC_AWARE_EXECUTOR_BEAN_NAME)
		@Primary
		public Executor mdcAwareCompletableFutureExecutor(
				@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) TaskExecutor applicationTaskExecutor) {
			return new MdcAwareExecutor(applicationTaskExecutor);
		}

	}

}
