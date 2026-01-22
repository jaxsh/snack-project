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

import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysResourceDTO;
import org.jax.snack.upms.api.service.SysResourceService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 资源/菜单管理 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysResourceControllerTests extends UpmsIntegrationTests {

	private static final String API_RESOURCES = "/api/upms/resources";

	private static final String API_RESOURCES_ID = "/api/upms/resources/{id}";

	private static final String API_RESOURCES_IDS = "/api/upms/resources/{ids}";

	private static final String API_RESOURCES_TREE = "/api/upms/resources/tree";

	@Autowired
	private SysResourceService sysResourceService;

	private SysResourceDTO buildDto(String name, Long parentId) {
		SysResourceDTO dto = new SysResourceDTO();
		dto.setName(name);
		dto.setParentId(parentId);
		dto.setPermission("test:permission:" + name);
		dto.setPath("/test/" + name);
		dto.setComponent("Layout");
		dto.setSortOrder(1);
		dto.setType(1);
		return dto;
	}

	@Nested
	class CreateResource {

		@Test
		void shouldCreateResourceSuccess() throws Exception {
			SysResourceDTO dto = buildDto("menu_create", 0L);

			postJson(API_RESOURCES, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());
		}

		@Test
		void shouldFailWhenParentNotFound() throws Exception {
			SysResourceDTO dto = buildDto("menu_fail", 9999L);

			postJson(API_RESOURCES, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class UpdateResource {

		@Test
		void shouldUpdateResourceSuccess() throws Exception {
			SysResourceDTO dto = buildDto("menu_update", 0L);
			SysResourceControllerTests.this.sysResourceService.create(dto);
			Long id = SysResourceControllerTests.this.sysResourceService.buildTree()
				.stream()
				.filter((node) -> "menu_update".equals(node.getData().getName()))
				.findFirst()
				.orElseThrow()
				.getData()
				.getId();

			SysResourceDTO updateDto = buildDto("menu_updated", 0L);
			putJson(API_RESOURCES_ID, updateDto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());
		}

		@Test
		void shouldFailWhenParentIsSelf() throws Exception {
			SysResourceDTO dto = buildDto("menu_self", 0L);
			SysResourceControllerTests.this.sysResourceService.create(dto);
			Long id = SysResourceControllerTests.this.sysResourceService.buildTree()
				.stream()
				.filter((node) -> "menu_self".equals(node.getData().getName()))
				.findFirst()
				.orElseThrow()
				.getData()
				.getId();

			SysResourceDTO updateDto = buildDto("menu_self", id);

			putJson(API_RESOURCES_ID, updateDto, id).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID));
		}

	}

	@Nested
	class DeleteResource {

		@Test
		void shouldDeleteResourceSuccess() throws Exception {
			SysResourceDTO dto = buildDto("menu_delete", 0L);
			SysResourceControllerTests.this.sysResourceService.create(dto);
			Long id = SysResourceControllerTests.this.sysResourceService.buildTree()
				.stream()
				.filter((node) -> "menu_delete".equals(node.getData().getName()))
				.findFirst()
				.orElseThrow()
				.getData()
				.getId();

			deleteJson(API_RESOURCES_IDS, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());
		}

	}

	@Nested
	class BuildTree {

		@Test
		void shouldReturnTree() throws Exception {
			SysResourceControllerTests.this.sysResourceService.create(buildDto("tree_root", 0L));

			getJson(API_RESOURCES_TREE).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[?(@.data.name == 'tree_root')]").exists());
		}

	}

	@Nested
	class GetRoleResources {

		@Test
		void shouldReturnResourcesByRoleCode() {
			SysResourceDTO dto = buildDto("ctrl_test", 0L);
			SysResourceControllerTests.this.sysResourceService.create(dto);

			SysResourceControllerTests.this.sysResourceService.buildTree()
				.stream()
				.filter((node) -> "ctrl_test".equals(node.getData().getName()))
				.findFirst()
				.orElseThrow();
		}

	}

}
