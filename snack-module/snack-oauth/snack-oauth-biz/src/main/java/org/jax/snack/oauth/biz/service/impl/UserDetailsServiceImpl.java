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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.repository.OAuthUserRepository;
import org.jax.snack.oauth.biz.security.OAuth2SecurityConstants;
import org.jax.snack.oauth.biz.security.PreAuthRestriction;
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

	private final OAuthUserRepository userRepository;

	private final List<PreAuthRestriction> restrictions;

	private final LoginAttemptService loginAttemptService;

	@Override
	@NullMarked public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		log.debug("Loading user by username: {}", username);
		QueryCondition condition = QueryCondition.builder().eq(OAuthUser.Fields.username, username).build();

		OAuthUser user = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		boolean accountLocked = isAccountLocked(user);

		if (Objects.equals(user.getLocked(), YesNoStatus.YES.getCode())) {
			this.loginAttemptService.setLockStatus(username, user.getLockUntil());
		}

		boolean accountExpired = Objects.equals(user.getExpired(), YesNoStatus.YES.getCode())
				|| (user.getExpireDate() != null && LocalDate.now().isAfter(user.getExpireDate()));

		return User.builder()
			.username(user.getUsername())
			.password(user.getPassword())
			.disabled(Objects.equals(user.getEnabled(), Status.DISABLED.getCode()))
			.accountLocked(accountLocked)
			.accountExpired(accountExpired)
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
	private boolean isAccountLocked(OAuthUser user) {
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
	 * 按优先级遍历已注册的 {@link PreAuthRestriction}，返回第一个适用限制的权限. 无限制时返回 {@code ROLE_USER}.
	 * @param user 用户实体
	 * @return 集合
	 */
	private Collection<? extends GrantedAuthority> getAuthorities(OAuthUser user) {
		return this.restrictions.stream()
			.filter((r) -> r.appliesTo(user))
			.findFirst()
			.map((r) -> List.<GrantedAuthority>of(new SimpleGrantedAuthority(r.getAuthority())))
			.orElseGet(() -> List.of(new SimpleGrantedAuthority(OAuth2SecurityConstants.ROLE_USER)));
	}

}
