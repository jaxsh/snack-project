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

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.jax.snack.framework.web.advice.GlobalExceptionAdvice;
import org.jax.snack.framework.web.advice.GlobalResponseBodyAdvice;
import org.jax.snack.framework.web.i18n.ParameterAwareAcceptHeaderLocaleResolver;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Web自动配置类.
 * <p>
 * 提供全局异常处理, 响应体处理, 国际化, 验证配置等功能.
 *
 * @author Jax Jiang
 */
@Configuration
@EnableConfigurationProperties(ValidationProperties.class)
public class WebAutoConfiguration implements WebMvcConfigurer {

	/**
	 * 全局异常处理器. 用于处理系统中的各种异常并转换为标准响应格式.
	 * @param messageSource 消息源, 用于国际化
	 * @return 全局异常处理器实例
	 */
	@Bean
	public GlobalExceptionAdvice exceptionAdvice(MessageSource messageSource) {
		return new GlobalExceptionAdvice(messageSource);
	}

	/**
	 * 全局响应体处理器. 用于统一处理控制器返回的响应体.
	 * @param jsonMapper json对象映射器
	 * @return 全局响应体处理器实例
	 */
	@Bean
	public GlobalResponseBodyAdvice responseBodyAdvice(JsonMapper jsonMapper) {
		return new GlobalResponseBodyAdvice(jsonMapper);
	}

	/**
	 * 本地化解析器. 支持通过请求参数和 Accept-Header 切换语言.
	 * @return 本地化解析器实例
	 */
	@Bean
	public LocaleResolver localeResolver() {
		return new ParameterAwareAcceptHeaderLocaleResolver();
	}

	/**
	 * 语言切换拦截器. 用于处理请求参数中的语言设置.
	 * @return 语言切换拦截器实例
	 */
	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		return new LocaleChangeInterceptor();
	}

	/**
	 * 添加拦截器. 注册语言切换拦截器到拦截器链中.
	 * @param registry 拦截器注册表
	 */
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(localeChangeInterceptor());
	}

	/**
	 * 配置Hibernate Validator是否快速失败.
	 * @param validationProperties 参数校验配置
	 * @return 配置定制器
	 */
	@Bean
	public ValidationConfigurationCustomizer hibernateFailFastCustomizer(ValidationProperties validationProperties) {
		return (configuration) -> configuration.addProperty(HibernateValidatorConfiguration.FAIL_FAST,
				validationProperties.getFailFast().toString());
	}

}
