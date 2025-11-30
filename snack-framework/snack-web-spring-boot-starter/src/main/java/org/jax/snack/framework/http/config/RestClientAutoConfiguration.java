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
import org.jax.snack.framework.http.handler.DefaultErrorWrappingInterceptor;
import org.jax.snack.framework.http.handler.DefaultStatusHandler;
import org.jax.snack.framework.http.handler.ErrorWrappingInterceptor;
import org.zalando.logbook.spring.LogbookClientHttpRequestInterceptor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * RestClient自动配置类. 用于配置RestClient相关的属性和Bean.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
public class RestClientAutoConfiguration {

	/**
	 * 创建一个默认的错误包装拦截器.
	 * @return 返回一个默认的错误包装拦截器实例
	 */
	@Bean
	@ConditionalOnMissingBean(ErrorWrappingInterceptor.class)
	public ErrorWrappingInterceptor errorWrappingInterceptor() {
		return new DefaultErrorWrappingInterceptor();
	}

	/**
	 * 创建一个默认的状态处理器.
	 * @return 返回一个默认的状态处理器实例
	 */
	@Bean
	@ConditionalOnMissingBean(CustomResponseErrorHandler.class)
	public CustomResponseErrorHandler defaultStatusHandler() {
		return new DefaultStatusHandler();
	}

	@Bean
	@Order(0)
	public RestClientCustomizer coreRestClientCustomizer(ErrorWrappingInterceptor errorWrappingInterceptor,
			CustomResponseErrorHandler defaultStatusHandler) {

		return (restClientBuilder) -> restClientBuilder.requestInterceptor(errorWrappingInterceptor)
			.defaultStatusHandler(defaultStatusHandler, defaultStatusHandler);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(LogbookClientHttpRequestInterceptor.class)
	protected static class LogbookConfiguration {

		@Bean
		@Order(1)
		public RestClientCustomizer logbookRestClientCustomizer(
				LogbookClientHttpRequestInterceptor logbookInterceptor) {

			return (restClientBuilder) -> restClientBuilder
				.requestInterceptors((interceptors) -> interceptors.add(0, logbookInterceptor));
		}

	}

}
