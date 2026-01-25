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

package org.jax.snack.oauth.server.security;

import java.time.ZonedDateTime;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.biz.entity.OAuth2User;
import org.jax.snack.oauth.biz.repository.OAuth2UserRepository;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.biz.service.LoginAttemptService;
import org.jax.snack.oauth.server.OAuthIntegrationTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;

/**
 * 登录锁定功能集成测试.
 *
 * @author Jax Jiang
 */
class LoginLockIntegrationTests extends OAuthIntegrationTests {

	@Autowired
	private OAuth2UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private LoginAttemptService loginAttemptService;

	@MockitoSpyBean
	private SecurityProperties securityProperties;

	private OAuth2User findByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, username).build();
		return this.userRepository.queryListByDsl(condition).stream().findFirst().orElseThrow();
	}

	private OAuth2User createTestUser(String username, String password) {
		OAuth2User user = new OAuth2User();
		user.setUsername(username);
		user.setPassword(this.passwordEncoder.encode(password));
		user.setEnabled(Status.ENABLED.getCode());
		user.setLocked(YesNoStatus.NO.getCode());
		user.setLockCount(0);
		this.userRepository.save(user);
		return user;
	}

	@Nested
	class FailureCount {

		@Test
		void shouldIncrementFailCountOnWrongPassword() throws Exception {
			String username = "failcount_" + System.currentTimeMillis();
			createTestUser(username, "password");

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("invalidPass"))
				.andExpect(unauthenticated());

			int failCount = LoginLockIntegrationTests.this.loginAttemptService.getFailCount(username);
			assertThat(failCount).isEqualTo(1);
		}

		@Test
		void shouldResetFailCountOnSuccessfulLogin() throws Exception {
			String username = "resetcount_" + System.currentTimeMillis();
			String password = "correctPwd";
			createTestUser(username, password);

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("error1"));
			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("error2"));

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());

			int failCount = LoginLockIntegrationTests.this.loginAttemptService.getFailCount(username);
			assertThat(failCount).isZero();
		}

	}

	@Nested
	class AccountLocking {

		@Test
		void shouldLockAccountAfterMaxAttempts() throws Exception {
			String username = "lockafter5_" + System.currentTimeMillis();
			String password = "mypassword";
			createTestUser(username, password);

			for (int i = 0; i < 5; i++) {
				LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("bad" + i));
			}

			assertThat(LoginLockIntegrationTests.this.loginAttemptService.isLocked(username)).isTrue();

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(unauthenticated());

			OAuth2User user = findByUsername(username);
			assertThat(user.getLocked()).isEqualTo(YesNoStatus.YES.getCode());
			assertThat(user.getLockCount()).isEqualTo(1);
		}

		@Test
		void shouldAllowLoginAfterLockExpires() throws Exception {
			String username = "lockexpire_" + System.currentTimeMillis();
			String password = "secret123";
			createTestUser(username, password);

			for (int i = 0; i < 5; i++) {
				LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("fail" + i));
			}

			OAuth2User user = findByUsername(username);
			user.setLockUntil(ZonedDateTime.now().minusMinutes(1));
			LoginLockIntegrationTests.this.userRepository.update(user);
			LoginLockIntegrationTests.this.loginAttemptService.clearLockStatus(username);

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());
		}

	}

	@Nested
	class TieredLocking {

		@Test
		void shouldIncreaseLockCountOnSecondLock() throws Exception {
			String username = "tiered_" + System.currentTimeMillis();
			String password = "tieredpwd";
			createTestUser(username, password);

			for (int i = 0; i < 5; i++) {
				LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("attempt" + i));
			}

			OAuth2User user = findByUsername(username);
			assertThat(user.getLockCount()).isEqualTo(1);

			user.setLockUntil(ZonedDateTime.now().minusMinutes(1));
			LoginLockIntegrationTests.this.userRepository.update(user);
			LoginLockIntegrationTests.this.loginAttemptService.clearLockStatus(username);

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());

			user = findByUsername(username);
			assertThat(user.getLockCount()).isZero();

			for (int i = 0; i < 5; i++) {
				LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("wrongAgain" + i));
			}

			user = findByUsername(username);
			assertThat(user.getLockCount()).isEqualTo(1);
		}

		@Test
		void shouldAllowNormalUserLogin() throws Exception {
			String username = "normaluser_" + System.currentTimeMillis();
			String password = "normalpass";
			createTestUser(username, password);

			OAuth2User userBefore = findByUsername(username);
			ZonedDateTime originalUpdateTime = userBefore.getUpdateTime();

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());

			OAuth2User userAfter = findByUsername(username);
			assertThat(userAfter.getLocked()).isEqualTo(YesNoStatus.NO.getCode());
			assertThat(userAfter.getLockCount()).isZero();
			assertThat(userAfter.getUpdateTime()).isEqualTo(originalUpdateTime);
		}

		@Test
		void shouldResetLockStatusWhenLockedUserLoginAfterExpiry() throws Exception {
			String username = "lockedexpiry_" + System.currentTimeMillis();
			String password = "lockedpwd";
			OAuth2User user = createTestUser(username, password);
			user.setLocked(YesNoStatus.YES.getCode());
			user.setLockCount(2);
			user.setLockUntil(ZonedDateTime.now().minusMinutes(1));
			LoginLockIntegrationTests.this.userRepository.update(user);

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());

			user = findByUsername(username);
			assertThat(user.getLocked()).isEqualTo(YesNoStatus.NO.getCode());
			assertThat(user.getLockCount()).isZero();
		}

		@Test
		void shouldDenyPermanentlyLockedUser() throws Exception {
			String username = "permlocked_" + System.currentTimeMillis();
			String password = "permpwd";
			OAuth2User user = createTestUser(username, password);
			user.setLocked(YesNoStatus.YES.getCode());
			user.setLockCount(5);
			user.setLockUntil(null);
			LoginLockIntegrationTests.this.userRepository.update(user);
			LoginLockIntegrationTests.this.loginAttemptService.setLockStatus(username, null);

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(unauthenticated());

			user = findByUsername(username);
			assertThat(user.getLocked()).isEqualTo(YesNoStatus.YES.getCode());
		}

	}

	@Nested
	class LockingDisabled {

		@BeforeEach
		void setUp() {
			doReturn(0).when(LoginLockIntegrationTests.this.securityProperties).getMaxLoginAttempts();
		}

		@Test
		void shouldNotLockWhenDisabled() throws Exception {
			String username = "nolock_" + System.currentTimeMillis();
			String password = "nolockpwd";
			createTestUser(username, password);

			for (int i = 0; i < 10; i++) {
				LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password("incorrect" + i))
					.andExpect(unauthenticated());
			}

			assertThat(LoginLockIntegrationTests.this.loginAttemptService.isLocked(username)).isFalse();

			OAuth2User user = findByUsername(username);
			assertThat(user.getLocked()).isEqualTo(YesNoStatus.NO.getCode());
			assertThat(user.getLockCount()).isZero();

			LoginLockIntegrationTests.this.mockMvc.perform(formLogin().user(username).password(password))
				.andExpect(authenticated());
		}

	}

}
