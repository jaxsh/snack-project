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

import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import org.jax.snack.oauth.biz.service.LoginAttemptService;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

/**
 * 基于缓存的用户状态预检查器.
 * <p>
 * 在密码验证前检查缓存中的锁定状态, 如果 UserDetails 显示锁定则同步到缓存.
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class CacheBasedUserDetailsChecker implements UserDetailsChecker {

	private final LoginAttemptService loginAttemptService;

	@Override
	public void check(UserDetails user) {
		String username = user.getUsername();

		if (!user.isEnabled()) {
			throw new DisabledException(SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractUserDetailsAuthenticationProvider.disabled", "User is disabled"));
		}

		if (!user.isAccountNonExpired()) {
			throw new AccountExpiredException(SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractUserDetailsAuthenticationProvider.expired", "User account has expired"));
		}

		if (this.loginAttemptService.isLocked(username)) {
			throw new LockedException(SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractUserDetailsAuthenticationProvider.locked"));
		}

		if (!user.isAccountNonLocked()) {
			ZonedDateTime lockUntil = this.loginAttemptService.getLockUntil(username);
			if (lockUntil == null) {
				lockUntil = ZonedDateTime.now().plusMinutes(5);
			}
			this.loginAttemptService.setLockStatus(username, lockUntil);
			throw new LockedException(SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractUserDetailsAuthenticationProvider.locked"));
		}
	}

}
