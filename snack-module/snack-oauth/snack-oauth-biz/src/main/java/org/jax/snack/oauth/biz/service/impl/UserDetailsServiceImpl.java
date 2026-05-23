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

package org.jax.snack.oauth.biz.service.impl;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.biz.entity.OAuth2User;
import org.jax.snack.oauth.biz.repository.OAuth2UserRepository;
import org.jax.snack.oauth.biz.security.OAuth2SecurityConstants;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.biz.service.LoginAttemptService;
import org.jspecify.annotations.NullMarked;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetails Service Implementation.
 * <p>
 * 使用 Spring Security 标准 User 类, 避免自定义类的序列化问题.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

	private final OAuth2UserRepository userRepository;

	private final SecurityProperties securityProperties;

	private final LoginAttemptService loginAttemptService;

	@Override
	@NullMarked public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.debug("Loading user by username: {}", username);
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, username).build();

		OAuth2User user = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		boolean accountLocked = isAccountLocked(user);

		if (Objects.equals(user.getLocked(), YesNoStatus.YES.getCode())) {
			this.loginAttemptService.setLockStatus(username, user.getLockUntil());
		}

		return User.builder()
			.username(user.getUsername())
			.password(user.getPassword())
			.disabled(Objects.equals(user.getEnabled(), Status.DISABLED.getCode()))
			.accountLocked(accountLocked)
			.accountExpired(Objects.equals(user.getExpired(), YesNoStatus.YES.getCode()))
			.credentialsExpired(false)
			.authorities(getAuthorities(user))
			.build();
	}

	/**
	 * 判断账户是否处于锁定状态.
	 * <p>
	 * 考虑时限锁定: 若 lock_until 已过期则视为未锁定.
	 * @param user 用户实体
	 * @return 是否锁定
	 */
	private boolean isAccountLocked(OAuth2User user) {
		if (!Objects.equals(user.getLocked(), YesNoStatus.YES.getCode())) {
			return false;
		}

		ZonedDateTime lockUntil = user.getLockUntil();
		if (lockUntil == null) {
			return true;
		}

		return ZonedDateTime.now().isBefore(lockUntil);
	}

	/**
	 * 获取用户权限列表.
	 * <p>
	 * 初始密码用户或密码过期用户只拥有 SCOPE_pre_auth_reset 权限.
	 * @param user 用户实体
	 * @return 集合
	 */
	private Collection<? extends GrantedAuthority> getAuthorities(OAuth2User user) {
		boolean needsPasswordChange = isInitialPasswordUser(user) || isCredentialsExpired(user);
		if (needsPasswordChange) {
			return List.of(new SimpleGrantedAuthority(
					OAuth2SecurityConstants.SCOPE_PREFIX + OAuth2SecurityConstants.PRE_AUTH_RESET_SCOPE));
		}
		return List.of(new SimpleGrantedAuthority(OAuth2SecurityConstants.ROLE_USER));
	}

	/**
	 * 检查是否为初始密码用户.
	 * @param user 用户
	 * @return 是否为初始密码
	 */
	private boolean isInitialPasswordUser(OAuth2User user) {
		return this.securityProperties.isForceChangeInitialPassword()
				&& Objects.equals(user.getInitialPassword(), YesNoStatus.YES.getCode());
	}

	/**
	 * 检查凭证是否过期.
	 * @param user 用户
	 * @return 是否过期
	 */
	private boolean isCredentialsExpired(OAuth2User user) {
		return this.securityProperties.isCredentialsExpired(user.getLastPasswordResetTime());
	}

}
