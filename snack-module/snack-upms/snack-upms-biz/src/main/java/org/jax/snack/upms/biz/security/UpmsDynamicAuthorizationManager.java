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

package org.jax.snack.upms.biz.security;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 动态权限鉴权管理器.
 * <p>
 * 根据当前请求 URL 动态匹配所需权限, 并检查用户是否拥有该权限.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpmsDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final UpmsSecurityMetadataManager metadataManager;

	@NullMarked
	@Override
	public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		HttpServletRequest request = context.getRequest();

		String requiredPermission = findRequiredPermission(request);

		if (requiredPermission == null) {
			return new AuthorizationDecision(false);
		}

		boolean granted = isGranted(authentication.get(), requiredPermission);
		return new AuthorizationDecision(granted);
	}

	private String findRequiredPermission(HttpServletRequest request) {
		for (Map.Entry<RequestMatcher, String> entry : this.metadataManager.getPermissionRules().entrySet()) {
			if (entry.getKey().matches(request)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private boolean isGranted(Authentication authentication, String permission) {
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities()
					.stream()
					.map(GrantedAuthority::getAuthority)
					.anyMatch(permission::equals);
	}

}
