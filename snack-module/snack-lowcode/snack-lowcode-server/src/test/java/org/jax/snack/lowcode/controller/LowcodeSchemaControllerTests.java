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

package org.jax.snack.lowcode.controller;

import java.util.List;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.lowcode.LowcodeIntegrationTests;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.service.LowcodeSchemaService;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Schema 管理 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class LowcodeSchemaControllerTests extends LowcodeIntegrationTests {

	private static final String API_SCHEMAS = "/api/lowcode/schemas";

	private static final String API_SCHEMAS_ID = "/api/lowcode/schemas/{id}";

	private static final String API_SCHEMAS_IDS = "/api/lowcode/schemas/{ids}";

	private static final String API_SCHEMAS_QUERY = "/api/lowcode/schemas/query";

	private static final String API_SCHEMAS_PUBLISH = "/api/lowcode/schemas/{id}/publish";

	@Autowired
	private LowcodeSchemaService schemaService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private LowcodeSchemaDTO buildDto(String schemaName, String label) {
		LowcodeSchemaDTO dto = new LowcodeSchemaDTO();
		dto.setSchemaName(schemaName);
		dto.setResourcePath(schemaName.replace("_", "-"));
		dto.setTableName("lc_" + schemaName);
		dto.setLabel(label);

		LowcodeSchemaDTO.FieldDTO field = new LowcodeSchemaDTO.FieldDTO();
		field.setFieldName("name");
		field.setDbColumn("name");
		field.setTitle("名称");
		field.setLogicType("string");
		field.setLength(100);
		dto.setFields(List.of(field));

		return dto;
	}

	private LowcodeSchemaVO queryBySchemaName(String schemaName) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.schemaName, schemaName).build();
		PageResult<LowcodeSchemaVO> result = this.schemaService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("Schema not found: " + schemaName);
		}
		return result.getRecords().get(0);
	}

	private LowcodeSchemaVO queryPublishedBySchemaName(String schemaName) {
		QueryCondition condition = QueryCondition.builder()
			.eq(LowcodeSchema.Fields.schemaName, schemaName)
			.eq(LowcodeSchema.Fields.status, 1)
			.build();
		PageResult<LowcodeSchemaVO> result = this.schemaService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("Published Schema not found: " + schemaName);
		}
		return result.getRecords().get(0);
	}

	@Nested
	class CreateSchema {

		@Test
		void shouldCreateSuccess() throws Exception {
			String schemaName = "test_entity";
			LowcodeSchemaDTO dto = buildDto(schemaName, "测试实体");

			postJson(API_SCHEMAS, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			LowcodeSchemaVO vo = queryBySchemaName(schemaName);
			assertThat(vo).isNotNull();
			assertThat(vo.getLabel()).isEqualTo("测试实体");
		}

		@Test
		void shouldFailWhenDuplicate() throws Exception {
			String schemaName = "test_dup";
			LowcodeSchemaDTO dto = buildDto(schemaName, "重复实体");
			LowcodeSchemaControllerTests.this.schemaService.create(dto);

			postJson(API_SCHEMAS, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

		@Test
		void shouldFailWhenParamInvalid() throws Exception {
			LowcodeSchemaDTO dto = new LowcodeSchemaDTO();

			postJson(API_SCHEMAS, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("schemaName"));
		}

	}

	@Nested
	class QuerySchemas {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto("test_q1", "查询1"));
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto("test_q2", "查询2"));

			QueryCondition condition = QueryCondition.builder().size(10).like(LowcodeSchema.Fields.label, "查询").build();

			postJson(API_SCHEMAS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(2));
		}

	}

	@Nested
	class GetById {

		@Test
		void shouldReturnSchemaById() throws Exception {
			String schemaName = "test_get";
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto(schemaName, "获取测试"));
			LowcodeSchemaVO created = queryBySchemaName(schemaName);

			getJson(API_SCHEMAS_ID, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.records[0].label").value("获取测试"));
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(API_SCHEMAS_ID, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class UpdateSchema {

		@Test
		void shouldUpdateSuccess() throws Exception {
			String schemaName = "test_update";
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto(schemaName, "更新前"));
			LowcodeSchemaVO created = queryBySchemaName(schemaName);

			LowcodeSchemaDTO updateDto = buildDto(schemaName, "更新后");

			putJson(API_SCHEMAS_ID, updateDto, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			LowcodeSchemaVO updated = queryBySchemaName(schemaName);
			assertThat(updated.getLabel()).isEqualTo("更新后");
		}

	}

	@Nested
	class DeleteSchema {

		@Test
		void shouldDeleteSuccess() throws Exception {
			String schemaName = "test_delete";
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto(schemaName, "删除测试"));
			LowcodeSchemaVO created = queryBySchemaName(schemaName);

			deleteJson(API_SCHEMAS_IDS, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.schemaName, schemaName).build();
			assertThat(LowcodeSchemaControllerTests.this.schemaService.queryByDsl(condition).getRecords()).isEmpty();
		}

	}

	@Nested
	class PublishSchema {

		@BeforeEach
		void setUp() {
			tearDown();
		}

		@AfterEach
		void tearDown() {
			try {
				LowcodeSchemaControllerTests.this.jdbcTemplate.execute("DROP TABLE IF EXISTS lc_pub_success");
				LowcodeSchemaControllerTests.this.jdbcTemplate.execute("DROP TABLE IF EXISTS lc_pub_fail_status");
				LowcodeSchemaControllerTests.this.jdbcTemplate
					.execute("DELETE FROM lowcode_schema WHERE schema_name IN ('pub_success', 'pub_fail_status')");
				LowcodeSchemaControllerTests.this.jdbcTemplate.execute(
						"DELETE FROM lowcode_schema_history WHERE schema_name IN ('pub_success', 'pub_fail_status')");
			}
			catch (DataAccessException ignored) {
			}
		}

		@Test
		void shouldPublishSuccess() throws Exception {
			String schemaName = "pub_success";
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto(schemaName, "发布测试成功"));
			LowcodeSchemaVO created = queryBySchemaName(schemaName);

			postJsonNoBody(API_SCHEMAS_PUBLISH, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			LowcodeSchemaVO published = queryPublishedBySchemaName(schemaName);
			assertThat(published.getStatus()).isEqualTo(1);
		}

		@Test
		void shouldFailWhenStatusIncorrect() throws Exception {
			String schemaName = "pub_fail_status";
			LowcodeSchemaControllerTests.this.schemaService.create(buildDto(schemaName, "发布测试状态错误"));
			LowcodeSchemaVO created = queryBySchemaName(schemaName);

			LowcodeSchemaControllerTests.this.schemaService.publishSchema(created.getId());
			LowcodeSchemaVO published = queryPublishedBySchemaName(schemaName);

			postJsonNoBody(API_SCHEMAS_PUBLISH, published.getId()).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_STATUS_ERROR));
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			postJsonNoBody(API_SCHEMAS_PUBLISH, 99999L).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

}
