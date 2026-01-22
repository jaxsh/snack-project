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

package org.jax.snack.oauth.biz.security;

import java.util.Objects;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.GrantedAuthority;

/**
 * OAuth2 安全策略.
 * <p>
 * 集中管理强制修改密码等核心安全访问规则.
 *
 * @author Jax Jiang
 */
public final class OAuth2SecurityPolicy {

	private static final String SCOPE_PRE_AUTH_RESET = "SCOPE_pre_auth_reset";

	private static final String ROLE_PASSWORD_CHANGE_REQUIRED = "ROLE_PASSWORD_CHANGE_REQUIRED";

	private OAuth2SecurityPolicy() {
	}

	/**
	 * 配置授权规则.
	 * @param authorize 授权匹配注册表
	 */
	public static void configureAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
		authorize.requestMatchers("/api/oauth2/user/**")
			.authenticated()
			.requestMatchers("/api/**")
			.access((authentication, context) -> {
				var authorities = Objects.requireNonNull(authentication.get())
					.getAuthorities()
					.stream()
					.map(GrantedAuthority::getAuthority)
					.toList();
				boolean requiresChange = authorities.contains(SCOPE_PRE_AUTH_RESET)
						|| authorities.contains(ROLE_PASSWORD_CHANGE_REQUIRED);
				return new AuthorizationDecision(!requiresChange);
			});
	}

}
