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

package org.jax.snack.upms.controller;

import java.time.LocalDate;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.enums.UserGender;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysUserVO;
import org.jax.snack.upms.biz.client.OAuth2UserClient;
import org.jax.snack.upms.biz.entity.SysUser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户管理 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysUserControllerTests extends UpmsIntegrationTests {

	private static final String API_USERS = "/api/upms/users";

	private static final String API_USERS_ID = "/api/upms/users/{id}";

	private static final String API_USERS_IDS = "/api/upms/users/{ids}";

	private static final String API_USERS_QUERY = "/api/upms/users/query";

	@Autowired
	private SysUserService sysUserService;

	@MockitoBean
	private OAuth2UserClient oAuth2UserClient;

	private SysUserDTO buildDto(String username, String realName) {
		SysUserDTO dto = new SysUserDTO();
		dto.setUsername(username);
		dto.setRealName(realName);
		dto.setNickname(username + "_nick");
		dto.setGender(UserGender.MALE.getCode());
		dto.setBirthday(JsonNullable.of(LocalDate.of(1990, 1, 1)));
		return dto;
	}

	private SysUserVO queryByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, username).build();
		PageResult<SysUserVO> result = SysUserControllerTests.this.sysUserService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("User not found: " + username);
		}
		return result.getRecords().get(0);
	}

	@Nested
	class CreateUser {

		@Test
		void shouldCreateUserSuccess() throws Exception {
			String username = "user_create";
			SysUserDTO dto = buildDto(username, "Creator");

			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());

			postJson(API_USERS, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysUserVO vo = queryByUsername(username);
			assertThat(vo).isNotNull();
			assertThat(vo.getRealName()).isEqualTo("Creator");
			assertThat(vo.getGender()).isEqualTo(UserGender.MALE.getCode());
			assertThat(vo.getGenderLabel()).isEqualTo(UserGender.MALE.getName());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).create(any());
		}

		@Test
		void shouldFailWhenUsernameAvailable() throws Exception {
			String username = "user_fail";
			SysUserDTO dto = buildDto(username, "FailUser");

			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(dto);

			postJson(API_USERS, dto).andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

		@Test
		void shouldFailWhenParamInvalid() throws Exception {
			SysUserDTO dto = new SysUserDTO();

			postJson(API_USERS, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("username"));
		}

	}

	@Nested
	class QueryUsers {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto("user_q1", "QUser1"));
			SysUserControllerTests.this.sysUserService.create(buildDto("user_q2", "QUser2"));

			QueryCondition condition = QueryCondition.builder()
				.size(10)
				.like(SysUser.Fields.nickname, "user_q")
				.build();

			postJson(API_USERS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(2));
		}

	}

	@Nested
	class GetById {

		@Test
		void shouldReturnUserById() throws Exception {
			String username = "user_get";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Getter"));
			SysUserVO created = queryByUsername(username);

			getJson(API_USERS_ID, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.records[0].nickname").value(username + "_nick"))
				.andExpect(jsonPath("$.data.records[0].genderLabel").exists());
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(API_USERS_ID, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpectAll(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class UpdateUser {

		@Test
		void shouldUpdateUserSuccess() throws Exception {
			String username = "user_update";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Updater"));
			SysUserVO created = queryByUsername(username);

			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setNickname("UpdatedNick");
			updateDto.setGender(UserGender.FEMALE.getCode());

			putJson(API_USERS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysUserVO updated = queryByUsername(username);
			assertThat(updated.getNickname()).isEqualTo("UpdatedNick");
			assertThat(updated.getGender()).isEqualTo(UserGender.FEMALE.getCode());
			assertThat(updated.getGenderLabel()).isEqualTo("女");
		}

		@Test
		void shouldSyncStatusWhenDisableUser() throws Exception {
			String username = "user_disable";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Disable"));
			SysUserVO created = queryByUsername(username);

			ArgumentCaptor<OAuthUserDTO> captor = ArgumentCaptor.forClass(OAuthUserDTO.class);
			Mockito.doNothing()
				.when(SysUserControllerTests.this.oAuth2UserClient)
				.update(eq(username), captor.capture());

			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setStatus(0);

			putJson(API_USERS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).update(eq(username), any());
			assertThat(captor.getValue().getEnabled()).isEqualTo(0);
		}

		@Test
		void shouldNotSyncWhenNoRelevantFieldChanged() throws Exception {
			String username = "user_no_sync";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "NoSync"));
			SysUserVO created = queryByUsername(username);

			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setNickname("NoSyncNick");

			putJson(API_USERS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient, Mockito.never()).update(any(), any());
		}

		@Test
		void shouldSyncMobileEmailWhenUpdated() throws Exception {
			String username = "user_sync_contact";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Contact"));
			SysUserVO created = queryByUsername(username);

			ArgumentCaptor<OAuthUserDTO> captor = ArgumentCaptor.forClass(OAuthUserDTO.class);
			Mockito.doNothing()
				.when(SysUserControllerTests.this.oAuth2UserClient)
				.update(eq(username), captor.capture());

			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setMobile("13800000001");
			updateDto.setEmail("test@example.com");

			putJson(API_USERS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).update(eq(username), any());
			assertThat(captor.getValue().getMobile()).isEqualTo("13800000001");
			assertThat(captor.getValue().getEmail()).isEqualTo("test@example.com");
		}

		@Test
		void shouldSyncExpireDateWhenUpdated() throws Exception {
			String username = "user_sync_expire";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Expire"));
			SysUserVO created = queryByUsername(username);

			ArgumentCaptor<OAuthUserDTO> captor = ArgumentCaptor.forClass(OAuthUserDTO.class);
			Mockito.doNothing()
				.when(SysUserControllerTests.this.oAuth2UserClient)
				.update(eq(username), captor.capture());

			LocalDate futureDate = LocalDate.now().plusDays(30);
			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setExpireDate(JsonNullable.of(futureDate));

			putJson(API_USERS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).update(eq(username), any());
			assertThat(captor.getValue().getExpireDate().get()).isEqualTo(futureDate);
		}

	}

	@Nested
	class DeleteUser {

		@Test
		void shouldDeleteUserSuccess() throws Exception {
			String username = "user_delete";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).delete(username);
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Deleter"));
			SysUserVO created = queryByUsername(username);

			deleteJson(API_USERS_IDS, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.nickname, username + "_nick").build();
			assertThat(SysUserControllerTests.this.sysUserService.queryByDsl(condition).getRecords()).isEmpty();

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).delete(username);
		}

	}

	@Nested
	class UnlockUser {

		@Test
		void shouldCallOauthUpdateWithLockedFalse() throws Exception {
			String username = "user_unlock";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "Unlock"));
			SysUserVO created = queryByUsername(username);

			ArgumentCaptor<OAuthUserDTO> captor = ArgumentCaptor.forClass(OAuthUserDTO.class);
			Mockito.doNothing()
				.when(SysUserControllerTests.this.oAuth2UserClient)
				.update(eq(username), captor.capture());

			SysUserControllerTests.this.mockMvc
				.perform(patch("/api/upms/users/{id}/unlock", created.getId()).with(defaultJwt()).with(csrf()))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).update(eq(username), any());
			assertThat(captor.getValue().getLocked()).isEqualTo(YesNoStatus.NO.getCode());
		}

		@Test
		void shouldReturnErrorWhenUserNotFound() throws Exception {
			SysUserControllerTests.this.mockMvc
				.perform(patch("/api/upms/users/{id}/unlock", 99999L).with(defaultJwt()).with(csrf()))
				.andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class ResetPassword {

		@Test
		void shouldCallOauthUpdateWithPasswordAndInitialFlag() throws Exception {
			String username = "user_reset_pw";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "ResetPw"));
			SysUserVO created = queryByUsername(username);

			ArgumentCaptor<OAuthUserDTO> captor = ArgumentCaptor.forClass(OAuthUserDTO.class);
			Mockito.doNothing()
				.when(SysUserControllerTests.this.oAuth2UserClient)
				.update(eq(username), captor.capture());

			SysUserDTO dto = new SysUserDTO();
			dto.setPassword("NewPass@123");

			postJson("/api/upms/users/{id}/reset-password", dto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).update(eq(username), any());
			assertThat(captor.getValue().getPassword()).isNotBlank();
			assertThat(captor.getValue().getInitialPassword()).isEqualTo(YesNoStatus.YES.getCode());
		}

	}

	@Nested
	class DeleteUserSessions {

		@Test
		void shouldCallOauthDeleteSession() throws Exception {
			String username = "user_del_session";
			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).create(any());
			SysUserControllerTests.this.sysUserService.create(buildDto(username, "DelSession"));
			SysUserVO created = queryByUsername(username);

			Mockito.doNothing().when(SysUserControllerTests.this.oAuth2UserClient).revokeTokens(username);

			deleteJson("/api/upms/users/{id}/tokens", created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			Mockito.verify(SysUserControllerTests.this.oAuth2UserClient).revokeTokens(username);
		}

	}

}
