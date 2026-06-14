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

package org.jax.snack.oauth.biz.config;

import org.jax.snack.framework.oauth2.client.config.OAuth2ClientAutoConfiguration;
import org.jax.snack.framework.oauth2.client.config.OAuth2ClientProperties;
import org.jax.snack.oauth.biz.security.config.OAuthFormLoginCustomizer;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.biz.security.handler.BizAccessDeniedHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationFailureHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth 表单登录自动配置.
 * <p>
 * 仅在 OAuth2 Client 安全链存在时（BFF 模式）激活，将 {@link OAuthFormLoginCustomizer} 注册为 Bean， 使其能通过
 * {@code OAuth2ClientSecurityCustomizer} SPI 配置表单登录处理器和路径分派异常处理。
 * <p>
 * 独立 oauth-server 模式下（无 Client 安全链），由 {@code DefaultSecurityConfig} 接管。
 *
 * @author Jax Jiang
 */
@AutoConfiguration(after = OAuth2ClientAutoConfiguration.class)
@ConditionalOnBean(name = "oauth2ClientSecurityFilterChain")
public class OAuthFormLoginAutoConfiguration {

	/**
	 * 认证成功处理器.
	 * @param jsonMapper JSON 序列化器
	 * @return JsonAuthenticationSuccessHandler
	 */
	@Bean
	@ConditionalOnMissingBean
	public JsonAuthenticationSuccessHandler jsonAuthenticationSuccessHandler(JsonMapper jsonMapper) {
		return new JsonAuthenticationSuccessHandler(jsonMapper);
	}

	/**
	 * 认证失败处理器.
	 * @param jsonMapper JSON 序列化器
	 * @return JsonAuthenticationFailureHandler
	 */
	@Bean
	@ConditionalOnMissingBean
	public JsonAuthenticationFailureHandler jsonAuthenticationFailureHandler(JsonMapper jsonMapper) {
		return new JsonAuthenticationFailureHandler(jsonMapper);
	}

	/**
	 * 访问拒绝处理器.
	 * @param jsonMapper JSON 序列化器
	 * @return BizAccessDeniedHandler
	 */
	@Bean
	@ConditionalOnMissingBean
	public BizAccessDeniedHandler bizAccessDeniedHandler(JsonMapper jsonMapper) {
		return new BizAccessDeniedHandler(jsonMapper);
	}

	/**
	 * 表单登录定制器.
	 * <p>
	 * 通过 {@code OAuth2ClientSecurityCustomizer} SPI 注入到
	 * {@code oauth2ClientSecurityFilterChain}， 配置 JSON 认证处理器和路径分派入口点（/api/** → 401
	 * JSON，其他路径 → 302 OAuth2 授权）。
	 * <p>
	 * 同时将前端 base URL 写入 {@link SecurityProperties}，供 {@code PreAuthRestrictionFilter}
	 * 在请求时读取.
	 * @param clientProperties OAuth2 Client 配置
	 * @param securityProperties 安全策略属性
	 * @param successHandler 认证成功处理器
	 * @param failureHandler 认证失败处理器
	 * @param accessDeniedHandler 访问拒绝处理器
	 * @param jsonMapper JSON 序列化器
	 * @return OAuthFormLoginCustomizer
	 */
	@Bean
	@ConditionalOnMissingBean
	public OAuthFormLoginCustomizer oAuthFormLoginCustomizer(OAuth2ClientProperties clientProperties,
			SecurityProperties securityProperties, JsonAuthenticationSuccessHandler successHandler,
			JsonAuthenticationFailureHandler failureHandler, BizAccessDeniedHandler accessDeniedHandler,
			JsonMapper jsonMapper) {
		if (!StringUtils.hasText(securityProperties.getFrontendBaseUrl())) {
			String baseUrl = extractBaseUrl(clientProperties.getDefaultSuccessUrl());
			if (!StringUtils.hasText(baseUrl)) {
				baseUrl = extractBaseUrl(securityProperties.getLoginPage());
			}
			securityProperties.setFrontendBaseUrl(baseUrl);
		}
		return new OAuthFormLoginCustomizer(clientProperties, successHandler, failureHandler, accessDeniedHandler,
				jsonMapper);
	}

	private static String extractBaseUrl(String url) {
		UriComponents uri = UriComponentsBuilder.fromUriString(url).build();
		if (uri.getScheme() == null) {
			return "";
		}
		int port = uri.getPort();
		return uri.getScheme() + "://" + uri.getHost() + ((port > 0) ? ":" + port : "");
	}

}
