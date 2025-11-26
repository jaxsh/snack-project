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

package org.jax.snack.framework.web.advice;

import java.util.List;
import java.util.Map;

import org.jax.snack.framework.web.model.ApiResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 统一响应封装增强器测试.
 *
 * @author Jax Jiang
 */
@WebMvcTest
@Import({ GlobalResponseBodyAdvice.class, GlobalResponseBodyAdviceTests.DummyController.class })
class GlobalResponseBodyAdviceTests {

	private static final String JSON_PATH_CODE = "$.code";

	private static final String JSON_PATH_MSG = "$.msg";

	private static final String JSON_PATH_DATA = "$.data";

	private static final String JSON_PATH_SUCCESS = "$.success";

	private static final String SUCCESS_MSG = "Success";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions performJsonRequest(String url) throws Exception {
		return this.mockMvc.perform(get(url).contentType(MediaType.APPLICATION_JSON));
	}

	@Nested
	class WhenResponseShouldBeWrapped {

		@Test
		void shouldWrapStringResponse() throws Exception {
			performJsonRequest("/test/success").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA).value("success"));
		}

		@Test
		void shouldWrapDtoResponse() throws Exception {
			performJsonRequest("/test/dto").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA + ".id").value(1),
						jsonPath(JSON_PATH_DATA + ".name").value("Test User"));
		}

		@Test
		void shouldWrapListResponse() throws Exception {
			performJsonRequest("/test/list").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA).isArray(),
						jsonPath(JSON_PATH_DATA + "[0].id").value(1), jsonPath(JSON_PATH_DATA + "[1].id").value(2));
		}

		@Test
		void shouldWrapMapResponse() throws Exception {
			performJsonRequest("/test/map").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA + ".key1").value("value1"),
						jsonPath(JSON_PATH_DATA + ".key2").value("value2"));
		}

		@Test
		void shouldWrapNullResponse() throws Exception {
			performJsonRequest("/test/null").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA).doesNotExist());
		}

		@Test
		void shouldWrapEmptyObjectResponse() throws Exception {
			performJsonRequest("/test/empty-object").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).doesNotExist(), jsonPath(JSON_PATH_MSG).value(SUCCESS_MSG),
						jsonPath(JSON_PATH_SUCCESS).value(true), jsonPath(JSON_PATH_DATA + ".id").value(0),
						jsonPath(JSON_PATH_DATA + ".name").doesNotExist());
		}

	}

	@Nested
	class WhenResponseShouldNotBeWrapped {

		@Test
		void shouldNotWrapResponseEntity() throws Exception {
			performJsonRequest("/test/response-entity").andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string("raw response"));
		}

		@Test
		void shouldNotWrapApiResponse() throws Exception {
			performJsonRequest("/test/api-response").andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value("200"), jsonPath(JSON_PATH_MSG).value("Custom Message"),
						jsonPath(JSON_PATH_SUCCESS).value(false), jsonPath(JSON_PATH_DATA).value("custom data"));
		}

		@Test
		void shouldNotWrapTextPlainResponse() throws Exception {
			GlobalResponseBodyAdviceTests.this.mockMvc.perform(get("/test/plain-text"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
				.andExpect(content().string("plain text"));
		}

	}

	@RestController
	static class DummyController {

		@GetMapping(value = "/test/success", produces = MediaType.APPLICATION_JSON_VALUE)
		String success() {
			return "success";
		}

		@GetMapping(value = "/test/dto", produces = MediaType.APPLICATION_JSON_VALUE)
		TestDto dto() {
			return new TestDto(1L, "Test User");
		}

		@GetMapping(value = "/test/list", produces = MediaType.APPLICATION_JSON_VALUE)
		List<TestDto> list() {
			return List.of(new TestDto(1L, "User 1"), new TestDto(2L, "User 2"));
		}

		@GetMapping(value = "/test/map", produces = MediaType.APPLICATION_JSON_VALUE)
		Map<String, String> map() {
			return Map.of("key1", "value1", "key2", "value2");
		}

		@GetMapping(value = "/test/null", produces = MediaType.APPLICATION_JSON_VALUE)
		Object nullValue() {
			return null;
		}

		@GetMapping(value = "/test/empty-object", produces = MediaType.APPLICATION_JSON_VALUE)
		TestDto emptyObject() {
			return new TestDto(0L, null);
		}

		@GetMapping(value = "/test/response-entity", produces = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<String> responseEntity() {
			return ResponseEntity.ok("raw response");
		}

		@GetMapping(value = "/test/api-response", produces = MediaType.APPLICATION_JSON_VALUE)
		ApiResponse<String> apiResponse() {
			return new ApiResponse<>("200", "Custom Message", "custom data");
		}

		@GetMapping("/test/plain-text")
		String plainText() {
			return "plain text";
		}

	}

	record TestDto(Long id, String name) {
	}

	@SpringBootApplication
	static class DummyApplication {

	}

}
