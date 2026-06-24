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

import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.api.enums.ResourceType;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysResourceVO;

import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

/**
 * 动态权限鉴权管理器.
 * <p>
 * 根据请求 URL 从角色缓存实时推导权限, 角色变更后无需重新登录即可生效.
 *
 * @author Jax Jiang
 */
@Order(100)
@Component
@RequiredArgsConstructor
public class UpmsDynamicAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final UpmsSecurityMetadataManager metadataManager;

	private final SysUserService sysUserService;

	@Override
	public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		Authentication auth = authentication.get();

		if (!auth.isAuthenticated()) {
			return new AuthorizationDecision(false);
		}

		String requiredPermission = findRequiredPermission(context.getRequest());
		if (requiredPermission == null) {
			return new AuthorizationDecision(false);
		}

		boolean permitted = this.sysUserService.getResourcesByUsername(auth.getName())
			.stream()
			.filter((r) -> ResourceType.API.getCode().equals(r.getType()))
			.anyMatch((r) -> requiredPermission.equals(buildPermission(r)));

		return new AuthorizationDecision(permitted);
	}

	private String findRequiredPermission(HttpServletRequest request) {
		for (Map.Entry<RequestMatcher, String> entry : this.metadataManager.getPermissionRules().entrySet()) {
			if (entry.getKey().matches(request)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private String buildPermission(SysResourceVO resource) {
		return resource.getMethod().toUpperCase(Locale.ROOT) + ":" + resource.getPath();
	}

}
