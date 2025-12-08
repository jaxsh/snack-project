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

import java.util.Map;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.MockMvcTestSupport;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.enums.Status;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字典类型 Controller 集成测试.
 *
 * @author Jax Jiang
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/dict_type_test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SysDictTypeControllerTests extends MockMvcTestSupport {

	private static final String BASE_URL = "/api/upms/dict-types";

	private static final String SYS_STATUS = "sys_status";

	private static final String FIELD_DICT_TYPE = ".dictType";

	private static final String ID_PATH = "/{id}";

	@Nested
	class CreateDictType {

		@Test
		void shouldCreateAndVerifyData() throws Exception {
			String dictType = "test_type_" + System.currentTimeMillis();
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("新测试字典");
			dto.setDictType(dictType);
			dto.setStatus(Status.ENABLED.getCode());
			dto.setSortOrder(99);

			postJson(BASE_URL, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("dictType", Map.of(QueryOperator.EQ.getValue(), dictType)));
			postJson(BASE_URL + "/query", condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpectAll(PageResultMatchers.totalIs(1), PageResultMatchers.record(0, ".dictName", "新测试字典"),
						PageResultMatchers.record(0, ".sortOrder", 99));
		}

		@Test
		void shouldFailWhenDictTypeBlank() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("测试字典");
			dto.setStatus(Status.ENABLED.getCode());

			postJson(BASE_URL, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("dictType"));
		}

		@Test
		void shouldFailWhenDuplicateDictType() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("重复测试");
			dto.setDictType(SYS_STATUS);
			dto.setStatus(Status.ENABLED.getCode());

			postJson(BASE_URL, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

	}

	@Nested
	class GetDictTypeById {

		@Test
		void shouldReturnDictTypeWithCorrectFields() throws Exception {
			getJson(BASE_URL + ID_PATH, 1L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpectAll(PageResultMatchers.totalIs(1), PageResultMatchers.record(0, ".id", 1),
						PageResultMatchers.record(0, FIELD_DICT_TYPE, SYS_STATUS),
						PageResultMatchers.record(0, ".dictName", "系统状态"), PageResultMatchers.record(0, ".status", 1));
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(BASE_URL + ID_PATH, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class QueryDictTypes {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			QueryCondition condition = new QueryCondition();
			condition.setCurrent(1);
			condition.setSize(10);

			postJson(BASE_URL + "/query", condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isNotEmpty());
		}

		@Test
		void shouldFilterByStatus() throws Exception {
			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("status", Map.of(QueryOperator.EQ.getValue(), Status.DISABLED.getCode())));

			postJson(BASE_URL + "/query", condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpectAll(PageResultMatchers.totalIs(1),
						PageResultMatchers.record(0, FIELD_DICT_TYPE, "disabled_type"));
		}

	}

	@Nested
	class UpdateDictType {

		@Test
		void shouldUpdateAndVerifyData() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("修改后的系统状态");
			dto.setStatus(Status.DISABLED.getCode());
			dto.setRemark("已修改");

			putJson(BASE_URL + ID_PATH, dto, 1L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			getJson(BASE_URL + ID_PATH, 1L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpectAll(PageResultMatchers.record(0, ".dictName", "修改后的系统状态"),
						PageResultMatchers.record(0, ".status", 0), PageResultMatchers.record(0, ".remark", "已修改"));
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			SysDictTypeDTO dto = new SysDictTypeDTO();
			dto.setDictName("更新后的名称");

			putJson(BASE_URL + ID_PATH, dto, 99999L).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class DeleteDictType {

		@Test
		void shouldDeleteAndVerifyRemoved() throws Exception {
			getJson(BASE_URL + ID_PATH, 2L).andDo(print())
				.andExpect(status().isOk())
				.andExpect(PageResultMatchers.totalIs(1));

			deleteJson(BASE_URL + ID_PATH, 2L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			getJson(BASE_URL + ID_PATH, 2L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isEmpty());
		}

	}

}
