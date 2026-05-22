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
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationFailureHandler;
import org.jax.snack.oauth.biz.security.handler.JsonAuthenticationSuccessHandler;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * 表单登录定制器.
 * <p>
 * 挂载 JSON 认证处理器，并将未认证请求的入口点重定向到 OAuth2 授权端点.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class OAuthFormLoginCustomizer implements OAuth2ClientSecurityCustomizer {

	private final OAuth2ClientProperties clientProperties;

	private final JsonAuthenticationSuccessHandler successHandler;

	private final JsonAuthenticationFailureHandler failureHandler;

	@Override
	public void customize(HttpSecurity http) {
		http.formLogin((form) -> form.successHandler(this.successHandler).failureHandler(this.failureHandler));
		String oauthEntryPoint = "/oauth2/authorization/" + this.clientProperties.getDefaultRegistrationId();
		http.exceptionHandling(
				(ex) -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(oauthEntryPoint)));
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}
