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

import org.hamcrest.Matchers;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.lowcode.LowcodeIntegrationTests;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据类型 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class LowcodeDataTypeControllerTests extends LowcodeIntegrationTests {

	private static final String API_DATA_TYPES = "/api/lowcode/meta/data-types";

	private static final String API_ENTITY_TEMPLATE = "/api/lowcode/meta/entity-template";

	private static final String API_FIELD_TEMPLATE = "/api/lowcode/meta/field-template";

	@Nested
	class GetDataTypes {

		@Test
		void shouldReturnEnabledDataTypes() throws Exception {
			getJson(API_DATA_TYPES).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data.length()").value(Matchers.greaterThan(0)));
		}

	}

	@Nested
	class GetEntityTemplate {

		@Test
		void shouldReturnEntityTemplateSchema() throws Exception {
			getJson(API_ENTITY_TEMPLATE).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").exists())
				.andExpect(jsonPath("$.data.$schema").exists());
		}

	}

	@Nested
	class GetFieldTemplate {

		@Test
		void shouldReturnFieldTemplateSchema() throws Exception {
			getJson(API_FIELD_TEMPLATE).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data").exists())
				.andExpect(jsonPath("$.data.$schema").exists());
		}

	}

}
