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

package org.jax.snack.oauth.biz.security.mfa;

import dev.samstevens.totp.code.CodeVerifier;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.repository.OAuthUserRepository;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TOTP MFA 提供者.
 * <p>
 * 读取 {@code oauth_user.mfa_secret} 后在本地完成时间算法验证，零跨模块调用.
 *
 * @author Jax Jiang
 */
@ConditionalOnProperty(name = "snack.mfa.type", havingValue = "TOTP", matchIfMissing = true)
@Component
@RequiredArgsConstructor
public class TotpMfaProvider implements MfaProvider {

	private final OAuthUserRepository oauthUserRepository;

	private final CodeVerifier codeVerifier;

	@Override
	public void send(String username) {
	}

	@Override
	public boolean verify(String username, String code) {
		return this.oauthUserRepository
			.queryListByDsl(QueryCondition.builder().eq(OAuthUser.Fields.username, username).build())
			.stream()
			.findFirst()
			.filter((u) -> StringUtils.hasText(u.getMfaSecret()))
			.map((u) -> this.codeVerifier.isValidCode(u.getMfaSecret(), code))
			.orElse(false);
	}

}
