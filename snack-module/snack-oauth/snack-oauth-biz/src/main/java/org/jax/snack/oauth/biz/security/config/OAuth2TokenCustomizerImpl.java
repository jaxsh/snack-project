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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * OAuth2 Token 定制器实现.
 * <p>
 * 实现作用域隔离模式，确保首次登录用户只能执行有限操作。
 *
 * @author Jax Jiang
 */
@Component
public class OAuth2TokenCustomizerImpl implements OAuth2TokenCustomizer<JwtEncodingContext> {

	private static final String SCOPE_PRE_AUTH_RESET = "pre_auth_reset";

	private static final String ROLE_PASSWORD_CHANGE_REQUIRED = "ROLE_PASSWORD_CHANGE_REQUIRED";

	private static final int RESTRICTED_TOKEN_TTL_MINUTES = 5;

	@Override
	public void customize(JwtEncodingContext context) {
		if (!isAccessTokenOrIdToken(context)) {
			return;
		}

		if (requiresPasswordChange(context.getPrincipal())) {
			context.getClaims().claim("scope", new HashSet<>(Set.of(SCOPE_PRE_AUTH_RESET)));
			context.getClaims().claim("authorities", Collections.emptyList());
			context.getClaims().expiresAt(Instant.now().plus(RESTRICTED_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
		}
	}

	/**
	 * 检查用户是否需要修改密码.
	 * @param principal 认证信息
	 * @return 是否需要修改密码
	 */
	private boolean requiresPasswordChange(Authentication principal) {
		return principal.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(ROLE_PASSWORD_CHANGE_REQUIRED::equals);
	}

	private boolean isAccessTokenOrIdToken(JwtEncodingContext context) {
		String tokenType = context.getTokenType().getValue();
		return OAuth2TokenType.ACCESS_TOKEN.getValue().equals(tokenType)
				|| OidcParameterNames.ID_TOKEN.equals(tokenType);
	}

}
