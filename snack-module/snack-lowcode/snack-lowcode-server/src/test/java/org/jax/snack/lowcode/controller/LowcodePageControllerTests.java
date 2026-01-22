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
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.lowcode.LowcodeIntegrationTests;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.dto.SavePageRequest;
import org.jax.snack.lowcode.api.service.LowcodePageService;
import org.jax.snack.lowcode.api.service.LowcodeSchemaService;
import org.jax.snack.lowcode.api.vo.LowcodePageVO;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 页面配置 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class LowcodePageControllerTests extends LowcodeIntegrationTests {

	private static final String API_PAGES = "/api/lowcode/pages";

	private static final String API_PAGES_BY_ID = "/api/lowcode/pages/{schemaId}/{type}";

	private static final String API_PAGES_DELETE = "/api/lowcode/pages/{id}";

	@Autowired
	private LowcodePageService pageService;

	@Autowired
	private LowcodeSchemaService schemaService;

	private Long testSchemaId;

	private LowcodeSchemaDTO buildSchemaDto(String schemaName) {
		LowcodeSchemaDTO dto = new LowcodeSchemaDTO();
		dto.setSchemaName(schemaName);
		dto.setResourcePath(schemaName.replace("_", "-"));
		dto.setTableName("lc_" + schemaName);
		dto.setLabel(schemaName);

		LowcodeSchemaDTO.FieldDTO field = new LowcodeSchemaDTO.FieldDTO();
		field.setFieldName("name");
		field.setDbColumn("name");
		field.setTitle("名称");
		field.setLogicType("string");
		field.setLength(100);
		dto.setFields(List.of(field));

		return dto;
	}

	private Long createTestSchema(String schemaName) {
		this.schemaService.create(buildSchemaDto(schemaName));
		QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.schemaName, schemaName).build();
		LowcodeSchemaVO schema = this.schemaService.queryByDsl(condition).getRecords().get(0);
		return schema.getId();
	}

	@BeforeEach
	void setUp() {
		this.testSchemaId = createTestSchema("page_test_schema_" + System.currentTimeMillis());
	}

	private SavePageRequest buildRequest(String pageType) {
		SavePageRequest request = new SavePageRequest();
		request.setSchemaId(this.testSchemaId);
		request.setPageType(pageType);
		request.setPageName(pageType + " page");
		request.setPageSchema("{\"type\": \"object\"}");
		return request;
	}

	@Nested
	class QueryPages {

		@Test
		void shouldReturnEmptyListWhenNoPages() throws Exception {
			getJson(API_PAGES + "?schemaId=" + LowcodePageControllerTests.this.testSchemaId).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(0));
		}

		@Test
		void shouldReturnPagesForSchema() throws Exception {
			LowcodePageControllerTests.this.pageService.savePage(buildRequest("list"));

			getJson(API_PAGES + "?schemaId=" + LowcodePageControllerTests.this.testSchemaId).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.length()").value(1))
				.andExpect(jsonPath("$.data[0].pageType").value("list"));
		}

	}

	@Nested
	class GetPageById {

		@Test
		void shouldReturnPageBySchemaIdAndType() throws Exception {
			LowcodePageControllerTests.this.pageService.savePage(buildRequest("create"));

			getJson(API_PAGES_BY_ID, LowcodePageControllerTests.this.testSchemaId, "create").andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.pageType").value("create"));
		}

		@Test
		void shouldReturn404WhenPageNotFound() throws Exception {
			getJson(API_PAGES_BY_ID, 99999L, "nonexistent").andDo(print()).andExpect(status().isNotFound());
		}

	}

	@Nested
	class CreatePage {

		@Test
		void shouldCreatePageSuccess() throws Exception {
			SavePageRequest request = buildRequest("detail");

			postJson(API_PAGES, request).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").isNumber());

			LowcodePageVO page = LowcodePageControllerTests.this.pageService
				.getPage(LowcodePageControllerTests.this.testSchemaId, "detail")
				.orElse(null);
			assertThat(page).isNotNull();
			assertThat(page.getPageName()).isEqualTo("detail page");
		}

	}

	@Nested
	class DeletePage {

		@Test
		void shouldDeletePageSuccess() throws Exception {
			Long pageId = LowcodePageControllerTests.this.pageService.savePage(buildRequest("update"));

			deleteJson(API_PAGES_DELETE, pageId).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(LowcodePageControllerTests.this.pageService.getPage(LowcodePageControllerTests.this.testSchemaId,
					"update"))
				.isEmpty();
		}

	}

}
