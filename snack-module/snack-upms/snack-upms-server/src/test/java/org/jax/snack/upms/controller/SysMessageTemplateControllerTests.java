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
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysMessageTemplateDTO;
import org.jax.snack.upms.api.service.SysMessageTemplateService;
import org.jax.snack.upms.api.vo.SysMessageTemplateVO;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 消息模版集成测试.
 *
 * @author Jax Jiang
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysMessageTemplateControllerTests extends UpmsIntegrationTests {

	private static final String API_TEMPLATE = "/message-template";

	private static final String API_TEMPLATE_QUERY = "/message-template/query";

	private static final String API_TEMPLATE_ID = "/message-template/{id}";

	private static final String API_TEMPLATE_IDS = "/message-template/{ids}";

	private static final String TYPE_SMS = "SMS";

	@Autowired
	private SysMessageTemplateService service;

	private SysMessageTemplateDTO buildDto(String code, String type) {
		SysMessageTemplateDTO dto = new SysMessageTemplateDTO();
		dto.setTemplateCode(code);
		dto.setTemplateName("Test Template " + code);
		dto.setTemplateType(type);
		dto.setContent("Test Content");
		dto.setTemplateConfig(Map.of("provider", "aliyun"));
		dto.setStatus(Status.DISABLED.getCode());
		return dto;
	}

	@Nested
	@Order(1)
	class CreateTemplate {

		@Test
		void testSave() throws Exception {
			SysMessageTemplateDTO dto = buildDto("TEST_SAVE", TYPE_SMS);

			postJson(API_TEMPLATE, dto).andExpect(status().isOk());
		}

	}

	@Nested
	@Order(2)
	class QueryTemplate {

		@Test
		void testPage() throws Exception {
			SysMessageTemplateControllerTests.this.service.create(buildDto("TEST_QUERY_1", TYPE_SMS));
			SysMessageTemplateControllerTests.this.service.create(buildDto("TEST_QUERY_2", "EMAIL"));

			QueryCondition condition = QueryCondition.builder().eq("template_type", TYPE_SMS).build();

			postJson(API_TEMPLATE_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isNotEmpty())
				.andExpect(PageResultMatchers.record(0, ".templateCode", "TEST_QUERY_1"))
				.andExpect(PageResultMatchers.record(0, ".templateTypeLabel", "短信"));
		}

	}

	@Nested
	@Order(3)
	class GetById {

		@Test
		void testGetById() throws Exception {
			SysMessageTemplateDTO dto = buildDto("TEST_GET", TYPE_SMS);
			SysMessageTemplateControllerTests.this.service.create(dto);

			QueryCondition condition = QueryCondition.builder().eq("template_code", "TEST_GET").build();
			SysMessageTemplateVO vo = SysMessageTemplateControllerTests.this.service.queryByDsl(condition)
				.getRecords()
				.get(0);

			getJson(API_TEMPLATE_ID, vo.getId()).andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.records[0].id").value(vo.getId()));
		}

	}

	@Nested
	@Order(4)
	class UpdateTemplate {

		@Test
		void testUpdate() throws Exception {
			SysMessageTemplateDTO dto = buildDto("TEST_UPDATE", TYPE_SMS);
			SysMessageTemplateControllerTests.this.service.create(dto);

			QueryCondition condition = QueryCondition.builder().eq("template_code", "TEST_UPDATE").build();
			SysMessageTemplateVO vo = SysMessageTemplateControllerTests.this.service.queryByDsl(condition)
				.getRecords()
				.get(0);

			SysMessageTemplateDTO updateDto = buildDto("TEST_UPDATE", TYPE_SMS);
			updateDto.setTemplateName("Updated Template");

			putJson(API_TEMPLATE_ID, updateDto, vo.getId()).andExpect(status().isOk());

			SysMessageTemplateVO updated = SysMessageTemplateControllerTests.this.service.getById(vo.getId());
			assertThat(updated.getTemplateName()).isEqualTo("Updated Template");
		}

	}

	@Nested
	@Order(5)
	class DeleteTemplate {

		@Test
		void testRemove() throws Exception {
			SysMessageTemplateControllerTests.this.service.create(buildDto("TEST_DELETE", TYPE_SMS));

			QueryCondition condition = QueryCondition.builder().eq("template_code", "TEST_DELETE").build();
			SysMessageTemplateVO vo = SysMessageTemplateControllerTests.this.service.queryByDsl(condition)
				.getRecords()
				.get(0);

			deleteJson(API_TEMPLATE_IDS, vo.getId()).andExpect(status().isOk());

			SysMessageTemplateVO deleted = SysMessageTemplateControllerTests.this.service.getById(vo.getId());
			assertThat(deleted).isNull();
		}

	}

}
