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
import java.util.Set;

import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysResourceDTO;
import org.jax.snack.upms.api.dto.SysRoleDTO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.enums.UserGender;
import org.jax.snack.upms.api.service.SysResourceService;
import org.jax.snack.upms.api.service.SysRoleService;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.biz.client.OAuth2UserClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前用户自服务 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysProfileControllerTests extends UpmsIntegrationTests {

	private static final String API_USER = "/api/upms/user";

	private static final String API_USER_RESOURCES = "/api/upms/user/resources";

	@Autowired
	private SysUserService sysUserService;

	@Autowired
	private SysRoleService sysRoleService;

	@Autowired
	private SysResourceService sysResourceService;

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

	@Nested
	class GetUserInfo {

		@Test
		void shouldReturnUserInfo() throws Exception {
			String username = "profile_info_test";
			SysUserDTO userDto = buildDto(username, "Administrator");
			Mockito.doNothing().when(SysProfileControllerTests.this.oAuth2UserClient).create(any());
			SysProfileControllerTests.this.sysUserService.create(userDto);

			SysProfileControllerTests.this.mockMvc
				.perform(get(API_USER).with(defaultJwt().jwt((builder) -> builder.subject(username))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.username").value(username))
				.andExpect(jsonPath("$.data.realName").value("Administrator"));
		}

	}

	@Nested
	class GetUserResources {

		@Test
		void shouldReturnResources() throws Exception {
			String permCode = "sys:profile:view";
			SysResourceDTO resourceDto = new SysResourceDTO();
			resourceDto.setName("Profile Resource");
			resourceDto.setParentId(0L);
			resourceDto.setPermission(permCode);
			resourceDto.setType(2);
			SysProfileControllerTests.this.sysResourceService.create(resourceDto);

			Long resourceId = SysProfileControllerTests.this.sysResourceService.buildTree()
				.stream()
				.filter((node) -> "Profile Resource".equals(node.getData().getName()))
				.findFirst()
				.orElseThrow()
				.getData()
				.getId();

			String roleCode = "ROLE_PROFILE_RES_TEST";
			SysRoleDTO roleDto = new SysRoleDTO();
			roleDto.setRoleCode(roleCode);
			roleDto.setRoleName("Profile Resource Role");
			roleDto.setResourceIds(Set.of(resourceId));
			SysProfileControllerTests.this.sysRoleService.create(roleDto);

			String username = "profile_res_test";
			SysUserDTO userDto = buildDto(username, "ResUser");
			userDto.setRoleCodes(JsonNullable.of(Set.of(roleCode)));
			Mockito.doNothing().when(SysProfileControllerTests.this.oAuth2UserClient).create(any());
			SysProfileControllerTests.this.sysUserService.create(userDto);

			SysProfileControllerTests.this.mockMvc
				.perform(get(API_USER_RESOURCES).with(defaultJwt().jwt((builder) -> builder.subject(username))))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[?(@.permission == '%s')]", permCode).exists());
		}

	}

}
