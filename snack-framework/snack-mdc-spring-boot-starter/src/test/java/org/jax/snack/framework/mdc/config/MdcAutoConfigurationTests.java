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

package org.jax.snack.framework.mdc.config;

import java.util.Iterator;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import org.jax.snack.framework.mdc.MdcAutoConfiguration;
import org.jax.snack.framework.mdc.MdcAwareExecutor;
import org.jax.snack.framework.mdc.MdcInterceptor;
import org.jax.snack.framework.mdc.MdcLogbackConfigurer;
import org.jax.snack.framework.mdc.MdcTaskDecorator;
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 {@link MdcAutoConfiguration} 的自动配置逻辑.
 * <p>
 * 包括: 1. 默认 Bean 的加载. 2. 条件属性 {@code logging.mdc.enabled} 的控制. 3. 自定义 Bean 的替换. 4.
 * Logback 模式的动态修改（作为配置的副作用）.
 *
 * @author Jax Jiang
 */
class MdcAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class, MdcAutoConfiguration.class));

	@Test
	void shouldBackOffWhenDisabled() {
		this.contextRunner.withPropertyValues("logging.mdc.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(MdcLogbackConfigurer.class);
			assertThat(context).doesNotHaveBean(MdcTaskDecorator.class);
		});
	}

	@Test
	void shouldProvideDefaultBeansWhenEnabled() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MdcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(MdcLogbackConfigurer.class);
				assertThat(context).hasSingleBean(MdcTaskDecorator.class);
				assertThat(context).hasSingleBean(TraceIdGenerator.class);
			});
	}

	@Test
	void shouldAllowCustomTraceIdGenerator() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MdcAutoConfiguration.class))
			.withUserConfiguration(CustomTraceIdConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(TraceIdGenerator.class);
				assertThat(context.getBean(TraceIdGenerator.class)).isInstanceOf(CustomTraceIdGenerator.class);
			});
	}

	@Test
	void shouldCreateMdcAwareExecutorWhenTaskExecutorPresent() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class, MdcAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasBean("mdcAwareCompletableFutureExecutor");
				assertThat(context.getBean("mdcAwareCompletableFutureExecutor")).isInstanceOf(MdcAwareExecutor.class);
			});
	}

	@Test
	void shouldRegisterWebComponentsInServletEnvironment() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class, MdcAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(MdcInterceptor.class));
	}

	/**
	 * 验证 Logback 模式是否被正确修改.
	 * <p>
	 * 这是一个集成测试，因为它需要启动 Spring Context 来触发 LogbackConfigurer.
	 */
	@Nested
	@SpringBootTest(classes = { MdcAutoConfiguration.class, TaskExecutionAutoConfiguration.class },
			properties = { "logging.mdc.trace-id-pattern=[TID:%X{traceId}]" })
	class LogbackPatternConfigurationTest {

		@Test
		void shouldModifyLogbackPattern() {
			LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
			Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

			Iterator<Appender<ILoggingEvent>> iterator = rootLogger.iteratorForAppenders();
			while (iterator.hasNext()) {
				Appender<ILoggingEvent> appender = iterator.next();
				if (appender instanceof ConsoleAppender<?> consoleAppender
						&& consoleAppender.getEncoder() instanceof PatternLayoutEncoder encoder) {
					String pattern = encoder.getPattern();
					if (pattern != null && pattern.contains("[TID:%X{traceId}]")) {
						assertThat(pattern).contains("[TID:%X{traceId}]");
					}
				}
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTraceIdConfiguration {

		@Bean
		TraceIdGenerator traceIdGenerator() {
			return new CustomTraceIdGenerator();
		}

	}

	static class CustomTraceIdGenerator implements TraceIdGenerator {

		@Override
		public String generate() {
			return "custom";
		}

	}

}
