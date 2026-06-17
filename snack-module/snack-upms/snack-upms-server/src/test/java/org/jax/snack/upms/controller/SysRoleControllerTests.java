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

import java.util.Set;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysRoleDTO;
import org.jax.snack.upms.api.service.SysRoleService;
import org.jax.snack.upms.api.vo.SysRoleVO;
import org.jax.snack.upms.biz.entity.SysRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色管理 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysRoleControllerTests extends UpmsIntegrationTests {

	private static final String API_ROLES = "/api/upms/roles";

	private static final String API_ROLES_ID = "/api/upms/roles/{id}";

	private static final String API_ROLES_IDS = "/api/upms/roles/{ids}";

	private static final String API_ROLES_QUERY = "/api/upms/roles/query";

	@Autowired
	private SysRoleService sysRoleService;

	private SysRoleDTO buildDto(String roleCode) {
		SysRoleDTO dto = new SysRoleDTO();
		dto.setRoleCode(roleCode);
		dto.setRoleName("Test Role " + roleCode);
		dto.setRoleDesc("Test Description");
		return dto;
	}

	private SysRoleVO queryByRoleCode(String roleCode) {
		QueryCondition condition = QueryCondition.builder().eq(SysRole.Fields.roleCode, roleCode).build();
		PageResult<SysRoleVO> result = SysRoleControllerTests.this.sysRoleService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("Role not found: " + roleCode);
		}
		return result.getRecords().get(0);
	}

	@Nested
	class CreateRole {

		@Test
		void shouldCreateRoleSuccess() throws Exception {
			String roleCode = "ROLE_CREATE_" + System.currentTimeMillis();
			SysRoleDTO dto = buildDto(roleCode);

			postJson(API_ROLES, dto).andDo(print()).andExpect(status().isOk());

			SysRoleVO vo = queryByRoleCode(roleCode);
			assertThat(vo).isNotNull();
			assertThat(vo.getRoleCode()).isEqualTo(roleCode);
			assertThat(vo.getRoleName()).isEqualTo(dto.getRoleName());
		}

		@Test
		void shouldFailWhenRoleCodeExists() throws Exception {
			String roleCode = "ROLE_FAIL_TEST";
			SysRoleDTO dto = buildDto(roleCode);
			SysRoleControllerTests.this.sysRoleService.create(dto);

			postJson(API_ROLES, dto).andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

		@Test
		void shouldFailWhenParamInvalid() throws Exception {
			SysRoleDTO dto = new SysRoleDTO();

			postJson(API_ROLES, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID));
		}

	}

	@Nested
	class UpdateRole {

		@Test
		void shouldUpdateRoleSuccess() throws Exception {
			String roleCode = "ROLE_UPDATE_TEST";
			SysRoleDTO dto = buildDto(roleCode);
			SysRoleControllerTests.this.sysRoleService.create(dto);
			SysRoleVO created = queryByRoleCode(roleCode);

			SysRoleDTO updateDto = new SysRoleDTO();
			updateDto.setRoleCode(roleCode);
			updateDto.setRoleName("Updated Role Name");
			updateDto.setResourceIds(Set.of());

			putJson(API_ROLES_ID, updateDto, created.getId()).andDo(print()).andExpect(status().isOk());

			SysRoleVO updated = queryByRoleCode(roleCode);
			assertThat(updated.getRoleName()).isEqualTo("Updated Role Name");
		}

		@Test
		void shouldFailWhenRoleCodeChanged() throws Exception {
			String roleCode = "ROLE_CHANGE_TEST";
			SysRoleDTO dto = buildDto(roleCode);
			SysRoleControllerTests.this.sysRoleService.create(dto);
			SysRoleVO created = queryByRoleCode(roleCode);

			SysRoleDTO updateDto = new SysRoleDTO();
			updateDto.setRoleCode("ROLE_CHANGED");
			updateDto.setRoleName("Updated Role Name");

			putJson(API_ROLES_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID));
		}

	}

	@Nested
	class DeleteRole {

		@Test
		void shouldDeleteRoleSuccess() throws Exception {
			String roleCode = "ROLE_DELETE_TEST";
			SysRoleDTO dto = buildDto(roleCode);
			SysRoleControllerTests.this.sysRoleService.create(dto);
			SysRoleVO created = queryByRoleCode(roleCode);

			deleteJson(API_ROLES_IDS, created.getId()).andDo(print()).andExpect(status().isOk());

			QueryCondition condition = QueryCondition.builder().eq(SysRole.Fields.roleCode, roleCode).build();
			assertThat(SysRoleControllerTests.this.sysRoleService.queryByDsl(condition).getRecords()).isEmpty();
		}

	}

	@Nested
	class QueryRoles {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			SysRoleControllerTests.this.sysRoleService.create(buildDto("ROLE_Q1"));
			SysRoleControllerTests.this.sysRoleService.create(buildDto("ROLE_Q2"));

			QueryCondition condition = QueryCondition.builder()
				.size(10)
				.like(SysRole.Fields.roleCode, "ROLE_Q")
				.build();

			postJson(API_ROLES_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(2));
		}

	}

}
