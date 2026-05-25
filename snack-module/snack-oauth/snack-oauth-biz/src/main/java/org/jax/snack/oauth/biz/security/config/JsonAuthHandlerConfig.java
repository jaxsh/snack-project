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

package org.jax.snack.oauth.biz.security.config;

import org.jax.snack.framework.oauth2.client.config.OAuth2ClientProperties;
import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;
import org.jax.snack.oauth.biz.security.handler.BizAccessDeniedHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationFailureHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JSON 认证处理器配置.
 * <p>
 * 无条件注册 JSON 认证成功/失败处理器 Bean，单体和分布式模式下均生效. 当 oauth2-client-starter 存在时，额外注册
 * {@link OAuthFormLoginCustomizer}.
 *
 * @author Jax Jiang
 */
@Configuration
public class JsonAuthHandlerConfig {

	@Bean
	@ConditionalOnMissingBean
	public JsonAuthenticationSuccessHandler jsonAuthenticationSuccessHandler(JsonMapper jsonMapper) {
		return new JsonAuthenticationSuccessHandler(jsonMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	public JsonAuthenticationFailureHandler jsonAuthenticationFailureHandler(JsonMapper jsonMapper) {
		return new JsonAuthenticationFailureHandler(jsonMapper);
	}

	@Bean
	@ConditionalOnMissingBean
	public BizAccessDeniedHandler bizAccessDeniedHandler(JsonMapper jsonMapper) {
		return new BizAccessDeniedHandler(jsonMapper);
	}

	/**
	 * 仅当 oauth2-client-starter 在 classpath 时才注册 {@link OAuthFormLoginCustomizer}.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OAuth2ClientSecurityCustomizer.class)
	static class OAuthFormLoginCustomizerRegistrar {

		@Bean
		@ConditionalOnMissingBean
		OAuthFormLoginCustomizer oAuthFormLoginCustomizer(OAuth2ClientProperties clientProperties,
				JsonAuthenticationSuccessHandler successHandler, JsonAuthenticationFailureHandler failureHandler,
				BizAccessDeniedHandler accessDeniedHandler, JsonMapper jsonMapper) {
			return new OAuthFormLoginCustomizer(clientProperties, successHandler, failureHandler, accessDeniedHandler,
					jsonMapper);
		}

	}

}
