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

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * 预授权限制授权管理器.
 * <p>
 * 检查用户是否持有任意 {@link PreAuthRestriction} 对应的权限. 如果持有且访问非豁免路径，则拒绝访问. 通过
 * {@code List<PreAuthRestriction>} 注入动态构建受限权限集合， 新增限制类型时无需修改此类.
 *
 * @author Jax Jiang
 */
@Slf4j
@Order(1)
@Component
public class PreAuthAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final Set<String> restrictionAuthorities;

	public PreAuthAuthorizationManager(List<PreAuthRestriction> restrictions) {
		this.restrictionAuthorities = restrictions.stream()
			.map(PreAuthRestriction::getAuthority)
			.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public AuthorizationDecision authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		Authentication auth = authentication.get();
		boolean hasRestriction = auth.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(this.restrictionAuthorities::contains);
		return new AuthorizationDecision(!hasRestriction);
	}

}
