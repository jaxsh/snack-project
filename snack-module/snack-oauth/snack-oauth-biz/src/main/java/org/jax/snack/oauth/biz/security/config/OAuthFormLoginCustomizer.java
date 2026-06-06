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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.oauth2.client.config.OAuth2ClientProperties;
import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;
import org.jax.snack.oauth.biz.security.OAuth2SecurityConstants;
import org.jax.snack.oauth.biz.security.handler.BizAccessDeniedHandler;
import org.jax.snack.oauth.biz.security.handler.BizAuthenticationEntryPoint;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationFailureHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 表单登录定制器.
 * <p>
 * 挂载 JSON 认证处理器，并按路径分派未认证入口点：/api/** 返回 401 JSON，其他路径重定向至 OAuth2 授权端点.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class OAuthFormLoginCustomizer implements OAuth2ClientSecurityCustomizer {

	private final OAuth2ClientProperties clientProperties;

	private final SecurityProperties securityProperties;

	private final JsonAuthenticationSuccessHandler successHandler;

	private final JsonAuthenticationFailureHandler failureHandler;

	private final BizAccessDeniedHandler accessDeniedHandler;

	private final JsonMapper jsonMapper;

	@Override
	public void customize(HttpSecurity http) {
		http.formLogin((form) -> form.successHandler(this.successHandler).failureHandler(this.failureHandler));
		String oauthEntryPoint = "/oauth2/authorization/" + this.clientProperties.getDefaultRegistrationId();
		http.exceptionHandling((ex) -> ex
			.defaultAuthenticationEntryPointFor(new BizAuthenticationEntryPoint(this.jsonMapper, oauthEntryPoint),
					(request) -> request.getRequestURI().startsWith("/api/"))
			.defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(oauthEntryPoint),
					(request) -> true)
			.accessDeniedHandler(this.accessDeniedHandler));
		http.oauth2Login((oauth2) -> oauth2.successHandler(buildOAuth2SuccessHandler()));
	}

	private AuthenticationSuccessHandler buildOAuth2SuccessHandler() {
		String restrictedAuthority = OAuth2SecurityConstants.SCOPE_PREFIX
				+ OAuth2SecurityConstants.PRE_AUTH_RESET_SCOPE;
		String changePasswordUrl = toAbsoluteUrl(this.securityProperties.getChangePasswordPage(),
				this.clientProperties.getDefaultSuccessUrl());
		String defaultUrl = this.clientProperties.getDefaultSuccessUrl();
		return (req, res, auth) -> {
			boolean hasRestriction = auth.getAuthorities()
				.stream()
				.anyMatch((a) -> a.getAuthority().equals(restrictedAuthority));
			String target = hasRestriction ? changePasswordUrl : defaultUrl;
			new SimpleUrlAuthenticationSuccessHandler(target).onAuthenticationSuccess(req, res, auth);
		};
	}

	private static String toAbsoluteUrl(String path, String baseUrl) {
		if (UriComponentsBuilder.fromUriString(path).build().getScheme() != null) {
			return path;
		}
		if (UriComponentsBuilder.fromUriString(baseUrl).build().getScheme() == null) {
			return path;
		}
		return UriComponentsBuilder.fromUriString(baseUrl)
			.replacePath(null)
			.replaceQuery(null)
			.fragment(null)
			.build()
			.toUriString() + path;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
