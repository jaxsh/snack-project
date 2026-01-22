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

package org.jax.snack.framework.oauth2.client.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.oauth2.client.spi.LoginAuditHandler;
import org.jspecify.annotations.NonNull;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * 审计日志登出处理器.
 * <p>
 * 在用户登出时记录审计日志.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class AuditLogoutHandler implements LogoutHandler {

	private final LoginAuditHandler loginAuditHandler;

	@Override
	public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			Authentication authentication) {
		if (authentication == null) {
			return;
		}

		String username = extractUsername(authentication);
		if (username != null) {
			this.loginAuditHandler.recordLogout(username, request);
		}
	}

	/**
	 * 从 Authentication 中提取用户名.
	 * @param authentication 认证信息
	 * @return 用户名
	 */
	private String extractUsername(Authentication authentication) {
		Object principal = authentication.getPrincipal();

		if (principal instanceof OidcUser oidcUser) {
			return oidcUser.getSubject();
		}
		else if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}
		else if (principal instanceof String username) {
			return username;
		}

		return null;
	}

}
