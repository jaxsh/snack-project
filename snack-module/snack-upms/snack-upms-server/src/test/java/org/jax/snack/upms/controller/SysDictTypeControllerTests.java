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

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.service.SysDictTypeService;
import org.jax.snack.upms.api.vo.SysDictTypeVO;
import org.jax.snack.upms.biz.entity.SysDictType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字典类型 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysDictTypeControllerTests extends UpmsIntegrationTests {

	private static final String API_DICT_TYPES = "/api/upms/dict-types";

	private static final String API_DICT_TYPES_QUERY = "/api/upms/dict-types/query";

	private static final String API_DICT_TYPES_ID = "/api/upms/dict-types/{id}";

	private static final String API_DICT_TYPES_IDS = "/api/upms/dict-types/{ids}";

	@Autowired
	private SysDictTypeService dictTypeService;

	private SysDictTypeDTO buildDto(String name, String type, Integer status, Integer sortOrder) {
		SysDictTypeDTO dto = new SysDictTypeDTO();
		dto.setDictName(name);
		dto.setDictType(type);
		dto.setStatus(status);
		dto.setSortOrder(sortOrder);
		return dto;
	}

	private SysDictTypeVO queryByDictType(String dictType) {
		QueryCondition condition = QueryCondition.builder().eq(SysDictType.Fields.dictType, dictType).build();
		PageResult<SysDictTypeVO> result = this.dictTypeService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("DictType not found: " + dictType);
		}
		return result.getRecords().get(0);
	}

	private SysDictTypeVO queryById(Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysDictType.Fields.id, id).build();
		PageResult<SysDictTypeVO> result = this.dictTypeService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("DictType not found: " + id);
		}
		return result.getRecords().get(0);
	}

	private boolean existsById(Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysDictType.Fields.id, id).build();
		return !this.dictTypeService.queryByDsl(condition).getRecords().isEmpty();
	}

	@Nested
	class CreateDictType {

		@Test
		void shouldCreateAndVerifyData() throws Exception {
			String dictType = "test_type_" + System.currentTimeMillis();
			SysDictTypeDTO dto = buildDto("新测试字典", dictType, Status.ENABLED.getCode(), 99);

			postJson(API_DICT_TYPES, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = QueryCondition.builder().eq(SysDictType.Fields.dictType, dictType).build();
			postJson(API_DICT_TYPES_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".dictName", "新测试字典"))
				.andExpect(PageResultMatchers.record(0, ".sortOrder", 99));
		}

		@Test
		void shouldFailWhenDictTypeBlank() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("测试字典");
			dto.setStatus(Status.ENABLED.getCode());

			postJson(API_DICT_TYPES, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("dictType"));
		}

		@Test
		void shouldFailWhenDuplicateDictType() throws Exception {
			String dictType = "duplicate_test_" + System.currentTimeMillis();
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("重复测试", dictType, Status.ENABLED.getCode(), 0));

			SysDictTypeDTO dto = buildDto("重复测试", dictType, Status.ENABLED.getCode(), 0);

			postJson(API_DICT_TYPES, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

	}

	@Nested
	class GetDictTypeById {

		@Test
		void shouldReturnDictTypeWithCorrectFields() throws Exception {
			String dictType = "get_test_" + System.currentTimeMillis();
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("获取测试", dictType, Status.ENABLED.getCode(), 0));
			Long id = queryByDictType(dictType).getId();

			getJson(API_DICT_TYPES_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".dictType", dictType))
				.andExpect(PageResultMatchers.record(0, ".dictName", "获取测试"))
				.andExpect(PageResultMatchers.record(0, ".status", 1));
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(API_DICT_TYPES_ID, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class QueryDictTypes {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("分页1", "page1_" + System.currentTimeMillis(), Status.ENABLED.getCode(), 0));
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("分页2", "page2_" + System.currentTimeMillis(), Status.ENABLED.getCode(), 0));

			QueryCondition condition = QueryCondition.builder().page(1, 10).build();

			postJson(API_DICT_TYPES_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isNotEmpty());
		}

		@Test
		void shouldFilterByStatus() throws Exception {
			String disabledType = "disabled_" + System.currentTimeMillis();
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("禁用测试", disabledType, Status.DISABLED.getCode(), 0));

			QueryCondition condition = QueryCondition.builder()
				.eq(SysDictType.Fields.status, Status.DISABLED.getCode())
				.build();

			postJson(API_DICT_TYPES_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isNotEmpty())
				.andExpect(PageResultMatchers.record(0, ".dictType", disabledType));
		}

	}

	@Nested
	class UpdateDictType {

		@Test
		void shouldUpdateAndVerifyData() throws Exception {
			String dictType = "update_test_" + System.currentTimeMillis();
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("更新测试", dictType, Status.ENABLED.getCode(), 0));
			Long id = queryByDictType(dictType).getId();

			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("修改后的名称");
			dto.setStatus(Status.DISABLED.getCode());
			dto.setRemark("已修改");

			putJson(API_DICT_TYPES_ID, dto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysDictTypeVO updated = queryById(id);
			assertThat(updated.getDictName()).isEqualTo("修改后的名称");
			assertThat(updated.getStatus()).isEqualTo(Status.DISABLED.getCode());
			assertThat(updated.getRemark()).isEqualTo("已修改");
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("更新后的名称");

			putJson(API_DICT_TYPES_ID, dto, 99999L).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class DeleteDictType {

		@Test
		void shouldDeleteAndVerifyRemoved() throws Exception {
			String dictType = "delete_test_" + System.currentTimeMillis();
			SysDictTypeControllerTests.this.dictTypeService
				.create(buildDto("删除测试", dictType, Status.ENABLED.getCode(), 0));
			Long id = queryByDictType(dictType).getId();

			SysDictTypeVO before = queryById(id);
			assertThat(before).isNotNull();

			deleteJson(API_DICT_TYPES_IDS, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(existsById(id)).isFalse();
		}

	}

}
