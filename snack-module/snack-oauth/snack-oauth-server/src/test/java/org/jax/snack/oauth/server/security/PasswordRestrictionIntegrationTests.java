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

import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.repository.OAuthUserRepository;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jax.snack.oauth.server.OAuthIntegrationTests;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 密码限制及强制改密流程集成测试.
 * <p>
 * 涵盖初始密码限制、密码过期限制及其禁用场景.
 *
 * @author Jax Jiang
 */
class PasswordRestrictionIntegrationTests extends OAuthIntegrationTests {

	private static final String API_CLIENTS = "/api/oauth/clients/1";

	@Autowired
	private OAuthUserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockitoSpyBean
	private SecurityProperties securityProperties;

	private void createUser(String username, String password, boolean initialPassword, ZonedDateTime lastResetTime) {
		OAuthUser user = new OAuthUser();
		user.setUsername(username);
		user.setPassword(this.passwordEncoder.encode(password));
		user.setEnabled(Status.ENABLED.getCode());
		user.setLocked(YesNoStatus.NO.getCode());
		user.setLockCount(0);
		user.setInitialPassword(initialPassword ? YesNoStatus.YES.getCode() : YesNoStatus.NO.getCode());
		user.setLastPasswordResetTime((lastResetTime != null) ? lastResetTime : ZonedDateTime.now());
		this.userRepository.save(user);
	}

	private MockHttpSession loginAndGetSession(String username, String password) throws Exception {
		MvcResult result = this.mockMvc.perform(formLogin().user(username).password(password))
			.andExpect(authenticated())
			.andReturn();
		return (MockHttpSession) result.getRequest().getSession();
	}

	@Nested
	class NormalUser {

		@Test
		void shouldAccessAllApis() throws Exception {
			String username = "normal_" + System.currentTimeMillis();
			String password = "password";
			createUser(username, password, false, null);

			MockHttpSession session = loginAndGetSession(username, password);

			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(session))
				.andExpect(status().is2xxSuccessful());
		}

	}

	@Nested
	class InitialPasswordScenarios {

		@Test
		void shouldFollowChangeFlowWhenEnabled() throws Exception {
			doReturn(true).when(PasswordRestrictionIntegrationTests.this.securityProperties)
				.isForceChangeInitialPassword();

			String username = "init_flow_" + System.currentTimeMillis();
			String initialPass = "initial_pass";
			String newPass = "Newpass@123";
			createUser(username, initialPass, true, null);

			MockHttpSession session = loginAndGetSession(username, initialPass);
			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(session))
				.andExpect(status().isForbidden());

			OAuthUserDTO updateDto = new OAuthUserDTO();
			updateDto.setPassword(newPass);
			updateDto.setInitialPassword(YesNoStatus.NO.getCode());
			updateDto.setExpired(YesNoStatus.NO.getCode());
			PasswordRestrictionIntegrationTests.this.mockMvc
				.perform(put("/api/oauth/user/{username}", username).with(csrf())
					.with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_client")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(PasswordRestrictionIntegrationTests.this.jsonMapper.writeValueAsString(updateDto)))
				.andExpect(status().isOk());

			PasswordRestrictionIntegrationTests.this.mockMvc.perform(logout());
			MockHttpSession newSession = loginAndGetSession(username, newPass);
			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(newSession))
				.andExpect(status().is2xxSuccessful());
		}

		@Test
		void shouldNotRestrictWhenDisabled() throws Exception {
			doReturn(false).when(PasswordRestrictionIntegrationTests.this.securityProperties)
				.isForceChangeInitialPassword();

			String username = "init_disabled_" + System.currentTimeMillis();
			String password = "password";
			createUser(username, password, true, null);

			MockHttpSession session = loginAndGetSession(username, password);

			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(session))
				.andExpect(status().is2xxSuccessful());
		}

	}

	@Nested
	class PasswordExpirationScenarios {

		@Test
		void shouldFollowChangeFlowWhenExpired() throws Exception {
			doReturn(90).when(PasswordRestrictionIntegrationTests.this.securityProperties).getPasswordExpirationDays();

			String username = "expired_flow_" + System.currentTimeMillis();
			String initialPass = "expired_pass";
			String newPass = "Newpass@456";
			createUser(username, initialPass, false, ZonedDateTime.now().minusDays(100));

			MockHttpSession session = loginAndGetSession(username, initialPass);
			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(session))
				.andExpect(status().isForbidden());

			OAuthUserDTO updateDto = new OAuthUserDTO();
			updateDto.setPassword(newPass);
			updateDto.setInitialPassword(YesNoStatus.NO.getCode());
			updateDto.setExpired(YesNoStatus.NO.getCode());
			PasswordRestrictionIntegrationTests.this.mockMvc
				.perform(put("/api/oauth/user/{username}", username).with(csrf())
					.with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_client")))
					.contentType(MediaType.APPLICATION_JSON)
					.content(PasswordRestrictionIntegrationTests.this.jsonMapper.writeValueAsString(updateDto)))
				.andExpect(status().isOk());

			PasswordRestrictionIntegrationTests.this.mockMvc.perform(logout());
			MockHttpSession newSession = loginAndGetSession(username, newPass);
			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(newSession))
				.andExpect(status().is2xxSuccessful());
		}

		@Test
		void shouldNotRestrictWhenExpirationDisabled() throws Exception {
			doReturn(0).when(PasswordRestrictionIntegrationTests.this.securityProperties).getPasswordExpirationDays();

			String username = "expired_disabled_" + System.currentTimeMillis();
			String password = "password";
			createUser(username, password, false, ZonedDateTime.now().minusDays(100));

			MockHttpSession session = loginAndGetSession(username, password);

			PasswordRestrictionIntegrationTests.this.mockMvc.perform(get(API_CLIENTS).session(session))
				.andExpect(status().is2xxSuccessful());
		}

	}

}
