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
 * MDC (Mapped Diagnostic Context) 相关组件的自动配置类.
 * <p>
 * 该类会自动配置处理 traceId 所需的 Bean, 包括日志格式注入、HTTP 请求拦截、 以及异步和定时任务的上下文传播. 默认情况下启用, 可通过配置属性禁用.
 *
 * @author Jax Jiang
 * @since 2025-06-09
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "logging.mdc", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MdcProperties.class)
@org.springframework.boot.autoconfigure.AutoConfigureAfter(TaskExecutionAutoConfiguration.class)
public class MdcAutoConfiguration {

	private final MdcProperties properties;

	/**
	 * 创建 MdcLogbackConfigurer 的 Bean, 用于动态地将 traceId 注入到日志格式中.
	 * @return {@link MdcLogbackConfigurer} 的实例.
	 */
	@Bean
	public MdcLogbackConfigurer logbackConfigurer() {
		return new MdcLogbackConfigurer(this.properties);
	}

	/**
	 * 创建 MdcTaskDecorator 的 Bean, 确保 MDC 上下文能够跨 {@code @Async} 异步任务传播.
	 * @return {@link MdcTaskDecorator} 的实例.
	 */
	@Bean
	public MdcTaskDecorator mdcTaskDecorator() {
		return new MdcTaskDecorator();
	}

	/**
	 * 创建一个默认的 TraceIdGenerator Bean.
	 * <p>
	 * 如果用户在自己的配置中定义了 {@link TraceIdGenerator} 类型的 Bean, 此默认 Bean 将不会被创建, 从而实现了生成策略的可替换性.
	 * @return {@link TraceIdGenerator} 的默认实现.
	 */
	@Bean
	@ConditionalOnMissingBean
	public TraceIdGenerator traceIdGenerator() {
		return new UuidTraceIdGenerator();
	}

	/**
	 * 仅在 Web 应用环境中生效的 Web 相关配置.
	 * <p>
	 * 设计为静态内部类, 使其可以拥有独立的加载条件, 不影响非 Web 环境的应用.
	 */
	@Configuration
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	@RequiredArgsConstructor
	public static class MdcWebConfiguration implements WebMvcConfigurer {

		private final MdcProperties properties;

		private final TraceIdGenerator traceIdGenerator;

		/**
		 * 创建 MdcInterceptor 的 Bean, 负责处理 HTTP 请求的 MDC 上下文.
		 * @return {@link MdcInterceptor} 的实例.
		 */
		@Bean
		public MdcInterceptor mdcInterceptor() {
			return new MdcInterceptor(this.properties, this.traceIdGenerator);
		}

		/**
		 * 注册 MdcInterceptor, 并根据配置应用包含和排除规则.
		 * @param registry 拦截器注册表.
		 */
		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(this.mdcInterceptor())
				.addPathPatterns(this.properties.getIncludePatterns())
				.excludePathPatterns(this.properties.getExcludePatterns());
		}

	}

	/**
	 * 仅在 Classpath 中存在 {@link TaskExecutor} 时, 才激活的用于处理 {@code CompletableFuture} 的
	 * Executor 配置.
	 * <p>
	 * 此配置旨在提供一个即插即用的、支持 MDC 上下文传播的 {@link Executor}, 以解决在使用 {@code CompletableFuture}
	 * 的异步方法时 traceId 丢失的问题.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(TaskExecutor.class)
	@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(
			name = TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
	public static class MdcCompletableFutureExecutorConfiguration {

		/**
		 * 定义了我们提供的 MDC 增强版 Executor 的 Bean 名称. 方便在其他地方通过名称引用.
		 */
		public static final String MDC_AWARE_EXECUTOR_BEAN_NAME = "mdcAwareCompletableFutureExecutor";

		/**
		 * ... <b>核心功能:</b>
		 * <ul>
		 * <li>通过 {@code @Primary} 注解, 此 Bean 将成为注入 {@code Executor} 时的<b>首选</b>实例.
		 * 这使得开发者在代码中直接注入 {@code Executor} 时 (例如: {@code @Autowired Executor executor}),
		 * 无需使用 {@code @Qualifier} 即可默认获得此 MDC 增强的实例.</li>
		 * <li>它包装了 Spring Boot 默认的 {@code applicationTaskExecutor}, 从而复用了所有相关的线程池配置 (如:
		 * {@code spring.task.execution.*}).</li>
		 * </ul>
		 * <p>
		 * <b>重要提示:</b>
		 * <ul>
		 * <li>此 {@code @Primary} 注解<b>不会</b>影响 {@code @Async} 的行为, 因为 {@code @Async}
		 * 默认是按<b>名称</b>查找名为 'taskExecutor' 的 Bean (其别名为 'applicationTaskExecutor').</li>
		 * <li>用户仍然可以通过 {@code @Qualifier("applicationTaskExecutor")}
		 * 来精确注入原始的、未被包装的默认执行器.</li>
		 * </ul>
		 * @param applicationTaskExecutor the Spring Boot 自动配置的默认 TaskExecutor (bean name:
		 * 'applicationTaskExecutor'), 通过 {@code @Qualifier} 精确注入.
		 * @return 一个被 {@link MdcAwareExecutor} 包装过的、作为首选的 {@link Executor} 实例.
		 */
		@Bean(name = MDC_AWARE_EXECUTOR_BEAN_NAME)
		@ConditionalOnMissingBean(name = MDC_AWARE_EXECUTOR_BEAN_NAME)
		@Primary
		public Executor mdcAwareCompletableFutureExecutor(
				@Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME) Executor applicationTaskExecutor) {
			return new MdcAwareExecutor(applicationTaskExecutor);
		}

	}

}
