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

import lombok.RequiredArgsConstructor;
import org.jax.snack.oauth.biz.service.LoginAttemptService;
import org.jspecify.annotations.NullMarked;

import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 带缓存锁定检查的 UserDetailsService 装饰器.
 * <p>
 * 在查询数据库之前先检查缓存中的锁定状态, 避免不必要的数据库访问.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class LockCheckingUserDetailsService implements UserDetailsService {

	private final UserDetailsService delegate;

	private final LoginAttemptService loginAttemptService;

	@Override
	@NullMarked public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (this.loginAttemptService.isLocked(username)) {
			throw new LockedException(SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractUserDetailsAuthenticationProvider.locked"));
		}
		return this.delegate.loadUserByUsername(username);
	}

}
