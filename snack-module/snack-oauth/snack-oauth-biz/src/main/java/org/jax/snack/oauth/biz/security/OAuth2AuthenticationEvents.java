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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.oauth.biz.entity.OAuth2User;
import org.jax.snack.oauth.biz.repository.OAuth2UserRepository;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.biz.service.LoginAttemptService;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * OAuth2 认证事件监听器.
 * <p>
 * 处理登录成功/失败事件, 实现自动锁定和解锁逻辑.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationEvents {

	private final OAuth2UserRepository userRepository;

	private final SecurityProperties securityProperties;

	private final LoginAttemptService loginAttemptService;

	/**
	 * 处理认证失败事件.
	 * <p>
	 * 递增失败次数, 达到阈值时锁定账户.
	 * @param event 认证失败事件
	 */
	@EventListener
	public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof String username) {
			handleLoginFailure(username);
		}
	}

	/**
	 * 处理交互式认证成功事件.
	 * <p>
	 * 只在用户之前被锁定时才重置数据库状态, 始终清除缓存.
	 * @param event 交互式认证成功事件
	 */
	@EventListener
	public void onAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
		Authentication auth = event.getAuthentication();
		if (auth.getPrincipal() instanceof UserDetails userDetails) {
			handleLoginSuccess(userDetails);
		}
	}

	/**
	 * 处理登录失败.
	 * @param username 用户名
	 */
	private void handleLoginFailure(String username) {
		int maxAttempts = this.securityProperties.getMaxLoginAttempts();
		if (maxAttempts <= 0) {
			return;
		}

		int failCount = this.loginAttemptService.incrementFailCount(username);
		log.debug("Login failed for user: {}, fail count: {}", username, failCount);

		if (failCount >= maxAttempts) {
			lockUser(username);
			this.loginAttemptService.resetFailCount(username);
		}
	}

	/**
	 * 锁定用户.
	 * @param username 用户名
	 */
	private void lockUser(String username) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, username).build();

		OAuth2User user = this.userRepository.queryListByDsl(condition).stream().findFirst().orElse(null);
		if (user == null) {
			return;
		}

		int lockCount = (user.getLockCount() != null) ? user.getLockCount() + 1 : 1;
		ZonedDateTime lockUntil = calculateLockUntil(lockCount);

		user.setLocked(true);
		user.setLockCount(lockCount);
		user.setLockUntil(lockUntil);

		WhereCondition where = WhereCondition.builder().eq(OAuth2User.Fields.username, username).build();
		this.userRepository.updateByDsl(user, where);

		this.loginAttemptService.setLockStatus(username, lockUntil);

		if (lockUntil != null) {
			log.warn("User {} locked until {} (lock count: {})", username, lockUntil, lockCount);
		}
		else {
			log.warn("User {} permanently locked (lock count: {})", username, lockCount);
		}
	}

	/**
	 * 处理登录成功.
	 * <p>
	 * 只有缓存中有锁定记录时才查询 DB 进行重置.
	 * @param userDetails 用户详情
	 */
	private void handleLoginSuccess(UserDetails userDetails) {
		String username = userDetails.getUsername();

		ZonedDateTime lockUntil = this.loginAttemptService.getLockUntil(username);
		if (lockUntil != null || this.loginAttemptService.isLocked(username)) {
			resetLockStatus(username);
			log.debug("Login success for user: {}, reset lock status", username);
		}

		this.loginAttemptService.resetFailCount(username);
		this.loginAttemptService.clearLockStatus(username);
	}

	/**
	 * 重置用户锁定状态（解锁）.
	 * @param username 用户名
	 */
	private void resetLockStatus(String username) {
		WhereCondition where = WhereCondition.builder().eq(OAuth2User.Fields.username, username).build();

		Map<String, Object> setData = new HashMap<>();
		setData.put(OAuth2User.Fields.locked, false);
		setData.put(OAuth2User.Fields.lockCount, 0);
		setData.put(OAuth2User.Fields.lockUntil, null);
		this.userRepository.updateByDsl(setData, where);
	}

	/**
	 * 计算锁定截止时间.
	 * <p>
	 * 根据锁定次数使用阶梯锁定策略.
	 * @param lockCount 锁定次数
	 * @return 锁定截止时间, null 表示永久锁定
	 */
	private ZonedDateTime calculateLockUntil(int lockCount) {
		List<Integer> durations = this.securityProperties.getLockDurations();
		int index = Math.min(lockCount - 1, durations.size() - 1);
		int minutes = durations.get(index);

		if (minutes <= 0) {
			return null;
		}
		return ZonedDateTime.now().plusMinutes(minutes);
	}

}
