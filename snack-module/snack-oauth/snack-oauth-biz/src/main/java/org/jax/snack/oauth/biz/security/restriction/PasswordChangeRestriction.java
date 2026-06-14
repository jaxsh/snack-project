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

package org.jax.snack.oauth.biz.security.restriction;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.security.PreAuthRestriction;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 强制改密限制策略.
 * <p>
 * 初始密码用户或密码已过期时，分配 {@code SCOPE_pre_auth_reset} 权限并跳转至改密页.
 *
 * @author Jax Jiang
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class PasswordChangeRestriction implements PreAuthRestriction {

	private static final String SCOPE_KEY = "pre_auth_reset";

	private final SecurityProperties securityProperties;

	@Override
	public String getAuthority() {
		return "SCOPE_" + SCOPE_KEY;
	}

	@Override
	public String getPagePath() {
		return this.securityProperties.getPreAuthPages().get(SCOPE_KEY);
	}

	@Override
	public boolean appliesTo(OAuthUser user) {
		boolean isInitial = this.securityProperties.isForceChangeInitialPassword()
				&& Objects.equals(user.getInitialPassword(), YesNoStatus.YES.getCode());
		boolean isExpired = this.securityProperties.isCredentialsExpired(user.getLastPasswordResetTime());
		return isInitial || isExpired;
	}

	@Override
	public int getOrder() {
		return 1;
	}

}
