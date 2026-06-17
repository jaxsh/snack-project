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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.security.PreAuthRestriction;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.biz.security.mfa.MfaProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MFA 验证限制策略.
 * <p>
 * 用户开启 MFA 后，登录时分配 {@code SCOPE_pre_auth_mfa} 权限并跳转至 MFA 验证页.
 *
 * @author Jax Jiang
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class MfaVerificationRestriction implements PreAuthRestriction {

	private static final String SCOPE_KEY = "pre_auth_mfa";

	private final SecurityProperties securityProperties;

	private final ObjectProvider<MfaProvider> mfaProviderProvider;

	@Override
	public String getAuthority() {
		return "SCOPE_" + SCOPE_KEY;
	}

	@Override
	public String getPagePath() {
		return this.securityProperties.getPreAuthPages().get(SCOPE_KEY);
	}

	@Override
	public int getOrder() {
		return 2;
	}

	@Override
	public boolean appliesTo(OAuthUser user) {
		return YesNoStatus.YES.getCode().equals(user.getMfaEnabled());
	}

	@Override
	public void onApplied(String username) {
		MfaProvider provider = this.mfaProviderProvider.getIfAvailable();
		if (provider != null) {
			provider.send(username);
		}
	}

}
