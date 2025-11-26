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

package org.jax.snack.framework.http.config;

import org.jax.snack.framework.http.handler.CustomResponseErrorHandler;
import org.jax.snack.framework.http.handler.ErrorWrappingInterceptor;
import org.junit.jupiter.api.Test;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * RestClient 自动配置测试.
 * <p>
 * 验证 {@link RestClientAutoConfiguration} 是否正确注册了错误处理拦截器、默认状态处理器和 RestClient 定制器 Bean.
 *
 * @author Jax Jiang
 */
class RestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

	@Test
	void shouldRegisterErrorWrappingInterceptor() {
		this.contextRunner.withUserConfiguration(MockLogbookConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(ErrorWrappingInterceptor.class);
			assertThat(context).hasBean("errorWrappingInterceptor");
		});
	}

	@Test
	void shouldRegisterDefaultStatusHandler() {
		this.contextRunner.withUserConfiguration(MockLogbookConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CustomResponseErrorHandler.class);
			assertThat(context).hasBean("defaultStatusHandler");
		});
	}

	@Test
	void shouldRegisterClientCustomizer() {
		this.contextRunner.withUserConfiguration(MockLogbookConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestClientCustomizer.class);
			assertThat(context).hasBean("clientCustomizer");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MockLogbookConfiguration {

		@Bean
		LogbookClientHttpRequestInterceptor logbookClientHttpRequestInterceptor() {
			return mock(LogbookClientHttpRequestInterceptor.class);
		}

	}

}
