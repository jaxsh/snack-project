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

package org.jax.snack.oauth.server.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.service.OAuthUserService;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.repository.OAuthUserRepository;
import org.jax.snack.oauth.server.OAuthIntegrationTests;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OAuth2 用户管理 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class OAuthUserAdminControllerTests extends OAuthIntegrationTests {

	private static final String API_USER = "/api/oauth/user";

	private static final String PATH_USERNAME = "/{username}";

	@Autowired
	private OAuthUserService userService;

	@Autowired
	private OAuthUserRepository userRepository;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private JwtRequestPostProcessor scopeClientJwt() {
		return jwt().authorities(new SimpleGrantedAuthority("SCOPE_client"));
	}

	private void createTestUser(String username) {
		OAuthUserDTO dto = new OAuthUserDTO();
		dto.setUsername(username);
		this.userService.create(dto);
	}

	private OAuthUser findByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(OAuthUser.Fields.username, username).build();
		return this.userRepository.queryListByDsl(condition).stream().findFirst().orElseThrow();
	}

	private void insertFakeSession(String username) {
		this.jdbcTemplate.update(
				"INSERT INTO oauth_authorization (id, registered_client_id, principal_name, authorization_grant_type) VALUES (?,?,?,?)",
				"fake-" + username, "test-client", username, "authorization_code");
	}

	private int countSessions(String username) {
		Integer count = this.jdbcTemplate
			.queryForObject("SELECT COUNT(*) FROM oauth_authorization WHERE principal_name=?", Integer.class, username);
		return (count != null) ? count : 0;
	}

	@Nested
	class UpdateUser {

		@Test
		void shouldDisableUserAndRevokeSessions() throws Exception {
			String username = "test_disable";
			createTestUser(username);
			insertFakeSession(username);

			OAuthUserDTO dto = new OAuthUserDTO();
			dto.setEnabled(0);

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(put(API_USER + PATH_USERNAME, username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(dto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(findByUsername(username).getEnabled()).isEqualTo(0);
			assertThat(countSessions(username)).isEqualTo(0);
		}

		@Test
		void shouldEnableUser() throws Exception {
			String username = "test_enable";
			createTestUser(username);
			OAuthUserAdminControllerTests.this.jdbcTemplate.update("UPDATE oauth_user SET enabled=0 WHERE username=?",
					username);

			OAuthUserDTO dto = new OAuthUserDTO();
			dto.setEnabled(1);

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(put(API_USER + PATH_USERNAME, username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(dto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(findByUsername(username).getEnabled()).isEqualTo(1);
		}

		@Test
		void shouldUnlockUserAndClearLockFields() throws Exception {
			String username = "test_unlock";
			createTestUser(username);
			LocalDateTime lockUntil = ZonedDateTime.now().plusHours(1).toLocalDateTime();
			OAuthUserAdminControllerTests.this.jdbcTemplate.update(
					"UPDATE oauth_user SET locked=1, lock_count=5, lock_until=? WHERE username=?", lockUntil, username);

			OAuthUserDTO dto = new OAuthUserDTO();
			dto.setLocked(0);

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(put(API_USER + PATH_USERNAME, username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(dto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			OAuthUser user = findByUsername(username);
			assertThat(user.getLocked()).isEqualTo(0);
			assertThat(user.getLockCount()).isEqualTo(0);
			assertThat(user.getLockUntil()).isNull();
		}

		@Test
		void shouldResetPasswordAndRevokeSessions() throws Exception {
			String username = "test_reset_pw";
			createTestUser(username);
			insertFakeSession(username);

			OAuthUserDTO dto = new OAuthUserDTO();
			dto.setPassword("NewPass@123");
			dto.setInitialPassword(1);

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(put(API_USER + PATH_USERNAME, username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(dto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(findByUsername(username).getInitialPassword()).isEqualTo(1);
			assertThat(countSessions(username)).isEqualTo(0);
		}

		@Test
		void shouldExpireAccountBasedOnDate() throws Exception {
			String username = "test_expire_date";
			createTestUser(username);
			LocalDate yesterday = LocalDate.now().minusDays(1);

			OAuthUserDTO dto = new OAuthUserDTO();
			dto.setExpireDate(JsonNullable.of(yesterday));

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(put(API_USER + PATH_USERNAME, username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(toJson(dto)))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(findByUsername(username).getExpireDate()).isEqualTo(yesterday);
			assertThat(OAuthUserAdminControllerTests.this.userDetailsService.loadUserByUsername(username)
				.isAccountNonExpired()).isFalse();
		}

	}

	@Nested
	class RevokeTokens {

		@Test
		void shouldRevokeAllTokensForUser() throws Exception {
			String username = "test_del_sessions";
			createTestUser(username);
			insertFakeSession(username);
			insertFakeSession(username + "_2");
			OAuthUserAdminControllerTests.this.jdbcTemplate.update(
					"INSERT INTO oauth_authorization (id, registered_client_id, principal_name, authorization_grant_type) VALUES (?,?,?,?)",
					"fake2-" + username, "test-client", username, "authorization_code");

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(delete(API_USER + PATH_USERNAME + "/tokens", username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(countSessions(username)).isEqualTo(0);
		}

		@Test
		void shouldSucceedWhenNoTokenExists() throws Exception {
			String username = "test_no_session";
			createTestUser(username);

			OAuthUserAdminControllerTests.this.mockMvc
				.perform(delete(API_USER + PATH_USERNAME + "/tokens", username).with(scopeClientJwt())
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(countSessions(username)).isEqualTo(0);
		}

	}

}
