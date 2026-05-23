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

import java.util.Locale;

import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.config.WebAutoConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 层异常处理器测试. 验证 404/405/415 等 HTTP 层异常以 ApiResponse 格式返回.
 *
 * @author Jax Jiang
 */
@WebMvcTest
@Import({ WebAutoConfiguration.class, HttpExceptionAdviceTests.HttpEndpointController.class })
class HttpExceptionAdviceTests {

	private static final String JSON_PATH_CODE = "$.code";

	private static final String JSON_PATH_MSG = "$.msg";

	private static final String JSON_PATH_DATA = "$.data";

	private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MessageSource messageSource;

	private String getMessage(String code, Locale locale) {
		return this.messageSource.getMessage(code, null, locale);
	}

	@Nested
	class WhenResourceNotFound {

		@Test
		void shouldReturn404WithApiResponse() throws Exception {
			HttpExceptionAdviceTests.this.mockMvc.perform(get("/non-existent-path"))
				.andExpect(status().isNotFound())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.NOT_FOUND), jsonPath(JSON_PATH_MSG).exists(),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

		@Test
		void shouldSupportChineseLocale() throws Exception {
			HttpExceptionAdviceTests.this.mockMvc
				.perform(get("/non-existent-path").header(HEADER_ACCEPT_LANGUAGE, "zh-CN"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.NOT_FOUND, Locale.CHINA)));
		}

		@Test
		void shouldSupportEnglishLocale() throws Exception {
			HttpExceptionAdviceTests.this.mockMvc
				.perform(get("/non-existent-path").header(HEADER_ACCEPT_LANGUAGE, "en-US"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.NOT_FOUND, Locale.US)));
		}

	}

	@Nested
	class WhenMethodNotAllowed {

		@Test
		void shouldReturn405WithApiResponse() throws Exception {
			HttpExceptionAdviceTests.this.mockMvc.perform(post("/test/http/get-only"))
				.andExpect(status().isMethodNotAllowed())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID), jsonPath(JSON_PATH_MSG).exists(),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

	}

	@Nested
	class WhenMediaTypeNotSupported {

		@Test
		void shouldReturn415WithApiResponse() throws Exception {
			HttpExceptionAdviceTests.this.mockMvc
				.perform(post("/test/http/json-only").contentType(MediaType.TEXT_PLAIN).content("hello"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID), jsonPath(JSON_PATH_MSG).exists(),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

	}

	/**
	 * 测试用 Controller.
	 */
	@RestController
	static class HttpEndpointController {

		@GetMapping("/test/http/get-only")
		void getOnly() {
		}

		@PostMapping(value = "/test/http/json-only", consumes = MediaType.APPLICATION_JSON_VALUE)
		void jsonOnly(@RequestBody String body) {
		}

	}

}
