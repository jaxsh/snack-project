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
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysDictDataDTO;
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.enums.Status;
import org.jax.snack.upms.api.service.SysDictDataService;
import org.jax.snack.upms.api.service.SysDictTypeService;
import org.jax.snack.upms.api.vo.SysDictDataVO;
import org.jax.snack.upms.biz.entity.SysDictData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 字典数据 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysDictDataControllerTests extends UpmsIntegrationTests {

	private static final String API_DICT_DATA = "/api/upms/dict-data";

	private static final String API_DICT_DATA_QUERY = "/api/upms/dict-data/query";

	private static final String API_DICT_DATA_ID = "/api/upms/dict-data/{id}";

	private static final String API_DICT_DATA_IDS = "/api/upms/dict-data/{ids}";

	private static final String API_DICT_DATA_TYPE = "/api/upms/dict-data/type/{dictType}";

	@Autowired
	private SysDictTypeService dictTypeService;

	@Autowired
	private SysDictDataService dictDataService;

	private QueryCondition queryCondition(String dictType) {
		return QueryCondition.builder().eq(SysDictData.Fields.dictType, dictType).build();
	}

	private void createDictType(String type, String name, Integer status) {
		SysDictTypeDTO dto = new SysDictTypeDTO();
		dto.setDictType(type);
		dto.setDictName(name);
		dto.setStatus(status);
		this.dictTypeService.create(dto);
	}

	private Long createDictData(String dictType, String label, String value, Integer status) {
		SysDictDataDTO dto = new SysDictDataDTO();
		dto.setDictType(dictType);
		dto.setDictLabel(label);
		dto.setDictValue(value);
		dto.setStatus(status);
		this.dictDataService.create(dto);
		return queryDataByValue(dictType, value).getId();
	}

	private SysDictDataVO queryDataByValue(String dictType, String value) {
		QueryCondition condition = QueryCondition.builder()
			.eq(SysDictData.Fields.dictType, dictType)
			.eq(SysDictData.Fields.dictValue, value)
			.build();
		PageResult<SysDictDataVO> result = this.dictDataService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("DictData not found: " + dictType + "/" + value);
		}
		return result.getRecords().get(0);
	}

	private boolean existsDataById(Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysDictData.Fields.id, id).build();
		return !this.dictDataService.queryByDsl(condition).getRecords().isEmpty();
	}

	@Nested
	class CreateDictData {

		@Test
		void shouldCreateAndVerifyData() throws Exception {
			String dictType = "create_test_" + System.currentTimeMillis();
			createDictType(dictType, "测试类型", Status.ENABLED.getCode());

			String dictValue = "val_" + System.currentTimeMillis();
			SysDictDataDTO dto = new SysDictDataDTO();
			dto.setDictType(dictType);
			dto.setDictLabel("新测试项");
			dto.setDictValue(dictValue);
			dto.setStatus(Status.ENABLED.getCode());
			dto.setSortOrder(99);

			postJson(API_DICT_DATA, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = QueryCondition.builder().eq(SysDictData.Fields.dictValue, dictValue).build();
			postJson(API_DICT_DATA_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".dictLabel", "新测试项"))
				.andExpect(PageResultMatchers.record(0, ".sortOrder", 99));
		}

		@Test
		void shouldFailWhenDictTypeBlank() throws Exception {
			SysDictDataDTO dto = new SysDictDataDTO();
			dto.setDictLabel("测试项");
			dto.setDictValue("test");

			postJson(API_DICT_DATA, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("dictType"));
		}

		@Test
		void shouldFailWhenDuplicateValue() throws Exception {
			String dictType = "duplicate_test_" + System.currentTimeMillis();
			createDictType(dictType, "重复测试", Status.ENABLED.getCode());
			createDictData(dictType, "已有项", "1", Status.ENABLED.getCode());

			SysDictDataDTO dto = new SysDictDataDTO();
			dto.setDictType(dictType);
			dto.setDictLabel("重复项");
			dto.setDictValue("1");

			postJson(API_DICT_DATA, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

	}

	@Nested
	class GetDictDataById {

		@Test
		void shouldReturnDictDataWithCorrectFields() throws Exception {
			String dictType = "get_id_test_" + System.currentTimeMillis();
			createDictType(dictType, "ID获取测试", Status.ENABLED.getCode());
			Long id = createDictData(dictType, "测试项", "v1", Status.ENABLED.getCode());

			getJson(API_DICT_DATA_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".dictType", dictType))
				.andExpect(PageResultMatchers.record(0, ".dictLabel", "测试项"))
				.andExpect(PageResultMatchers.record(0, ".dictValue", "v1"));
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(API_DICT_DATA_ID, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class QueryDictData {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			String dictType = "page_test_" + System.currentTimeMillis();
			createDictType(dictType, "分页测试", Status.ENABLED.getCode());
			createDictData(dictType, "P1", "p1", Status.ENABLED.getCode());
			createDictData(dictType, "P2", "p2", Status.ENABLED.getCode());

			QueryCondition condition = queryCondition(dictType).toBuilder().current(1).size(10).build();

			postJson(API_DICT_DATA_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(2));
		}

		@Test
		void shouldFilterByDictType() throws Exception {
			String dictType = "filter_test_" + System.currentTimeMillis();
			createDictType(dictType, "过滤测试", Status.ENABLED.getCode());
			createDictData(dictType, "F1", "f1", Status.ENABLED.getCode());

			QueryCondition condition = queryCondition(dictType);

			postJson(API_DICT_DATA_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".dictType", dictType));
		}

	}

	@Nested
	class GetByDictType {

		@Test
		void shouldReturnEnabledDataOnly() throws Exception {
			String dictType = "type_filter_" + System.currentTimeMillis();
			createDictType(dictType, "类型获取测试", Status.ENABLED.getCode());
			createDictData(dictType, "E1", "e1", Status.ENABLED.getCode());
			createDictData(dictType, "D1", "d1", Status.DISABLED.getCode());

			getJson(API_DICT_DATA_TYPE, dictType).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray())
				.andExpect(ApiResponseMatchers.data(".length()", 1))
				.andExpect(ApiResponseMatchers.data("[0].dictValue", "e1"));
		}

		@Test
		void shouldReturnEmptyWhenTypeDisabled() throws Exception {
			String dictType = "disabled_type_" + System.currentTimeMillis();
			createDictType(dictType, "禁用类型测试", Status.DISABLED.getCode());
			createDictData(dictType, "V1", "v1", Status.ENABLED.getCode());

			getJson(API_DICT_DATA_TYPE, dictType).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray())
				.andExpect(ApiResponseMatchers.data(".length()", 0));
		}

		@Test
		void shouldReturnEmptyWhenTypeNotFound() throws Exception {
			getJson(API_DICT_DATA_TYPE, "not_exist").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray());
		}

	}

	@Nested
	class UpdateDictData {

		@Test
		void shouldUpdateAndVerifyData() throws Exception {
			String dictType = "update_test_" + System.currentTimeMillis();
			createDictType(dictType, "更新测试", Status.ENABLED.getCode());
			Long id = createDictData(dictType, "原名称", "old", Status.ENABLED.getCode());

			SysDictDataDTO dto = new SysDictDataDTO();
			dto.setDictType(dictType);
			dto.setDictLabel("修改后的名称");
			dto.setDictValue("old");
			dto.setStatus(Status.DISABLED.getCode());
			dto.setRemark("已修改");

			putJson(API_DICT_DATA_ID, dto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			getJson(API_DICT_DATA_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.record(0, ".dictLabel", "修改后的名称"))
				.andExpect(PageResultMatchers.record(0, ".status", 0))
				.andExpect(PageResultMatchers.record(0, ".remark", "已修改"));
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			SysDictDataDTO dto = new SysDictDataDTO();
			dto.setDictLabel("更新");

			putJson(API_DICT_DATA_ID, dto, 99999L).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class DeleteDictData {

		@Test
		void shouldDeleteAndVerifyRemoved() throws Exception {
			String dictType = "delete_test_" + System.currentTimeMillis();
			createDictType(dictType, "删除测试", Status.ENABLED.getCode());
			Long id = createDictData(dictType, "待删除", "del", Status.ENABLED.getCode());

			getJson(API_DICT_DATA_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpect(PageResultMatchers.totalIs(1));

			deleteJson(API_DICT_DATA_IDS, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(existsDataById(id)).isFalse();
		}

	}

}
