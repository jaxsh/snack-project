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

package org.jax.snack.framework.web.config;

import org.jax.snack.framework.web.advice.GlobalExceptionAdvice;
import org.jax.snack.framework.web.advice.GlobalResponseBodyAdvice;
import org.jax.snack.framework.web.i18n.ParameterAwareAcceptHeaderLocaleResolver;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Web 自动配置测试.
 * <p>
 * 验证 {@link WebAutoConfiguration} 是否正确注册了全局异常处理、响应体处理、国际化及验证相关的 Bean, 并确认其作为
 * {@link WebMvcConfigurer} 的功能.
 *
 * @author Jax Jiang
 */
class WebAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
		.withUserConfiguration(MessageSourceConfiguration.class, JsonMapperConfiguration.class,
				MockValidationPropertiesConfiguration.class);

	@Test
	void shouldRegisterGlobalExceptionAdvice() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GlobalExceptionAdvice.class);
			assertThat(context).hasBean("exceptionAdvice");
		});
	}

	@Test
	void shouldRegisterGlobalResponseBodyAdvice() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(GlobalResponseBodyAdvice.class);
			assertThat(context).hasBean("responseBodyAdvice");
		});
	}

	@Test
	void shouldRegisterLocaleResolver() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(LocaleResolver.class);
			assertThat(context).hasBean("localeResolver");
			assertThat(context.getBean("localeResolver")).isInstanceOf(ParameterAwareAcceptHeaderLocaleResolver.class);
		});
	}

	@Test
	void shouldRegisterLocaleChangeInterceptor() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(LocaleChangeInterceptor.class);
			assertThat(context).hasBean("localeChangeInterceptor");
		});
	}

	@Test
	void shouldRegisterWebAutoConfigurationAsWebMvcConfigurer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(WebAutoConfiguration.class);
			assertThat(context).hasSingleBean(WebMvcConfigurer.class); // Check if
																		// WebAutoConfiguration
																		// is registered
																		// as
																		// WebMvcConfigurer
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MessageSourceConfiguration {

		@Bean
		MessageSource messageSource() {
			return mock(MessageSource.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JsonMapperConfiguration {

		@Bean
		JsonMapper jsonMapper() {
			return mock(JsonMapper.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MockValidationPropertiesConfiguration {

		@Bean
		ValidationProperties validationProperties() {
			return mock(ValidationProperties.class);
		}

	}

}
