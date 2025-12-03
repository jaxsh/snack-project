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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.jax.snack.framework.common.exception.BusinessException;
import org.jax.snack.framework.common.exception.constants.ErrorCode;
import org.jax.snack.framework.http.exception.InterfaceBusinessException;
import org.jax.snack.framework.http.exception.InterfaceException;
import org.jax.snack.framework.web.config.WebAutoConfiguration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常处理器测试. 验证各种异常场景的统一处理和国际化功能.
 *
 * @author Jax Jiang
 */
@WebMvcTest
@Import({ WebAutoConfiguration.class, GlobalExceptionAdviceTests.NativeValidationController.class,
		GlobalExceptionAdviceTests.AopValidatedController.class })
class GlobalExceptionAdviceTests {

	private static final String JSON_PATH_CODE = "$.code";

	private static final String JSON_PATH_MSG = "$.msg";

	private static final String JSON_PATH_DATA = "$.data";

	private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

	private static final String URL_BUSINESS_EXCEPTION = "/test/business-exception";

	private static final String FIELD_NAME = "name";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MessageSource messageSource;

	private String getMessage(String code) {
		return getMessage(code, Locale.getDefault());
	}

	private String getMessage(String code, Locale locale) {
		return this.messageSource.getMessage(code, null, locale);
	}

	@Nested
	class WhenBusinessExceptionOccurs {

		@Test
		void shouldHandleBusinessExceptionWithDefaultLocale() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get(URL_BUSINESS_EXCEPTION))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.SYSTEM_ERROR),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR)),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

		@Test
		void shouldHandleBusinessExceptionWithEnglishLocale() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc
				.perform(get(URL_BUSINESS_EXCEPTION).header(HEADER_ACCEPT_LANGUAGE, "en-US"))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.SYSTEM_ERROR),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR, Locale.US)),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

	}

	@Nested
	class WhenInterfaceExceptionOccurs {

		@Test
		void shouldHandleInterfaceException() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/interface-exception"))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.INTERFACE_ERROR),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.INTERFACE_ERROR)),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

		@Test
		void shouldHandleInterfaceExceptionWithCause() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/interface-exception-with-cause"))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.INTERFACE_ERROR),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.INTERFACE_ERROR)),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

		@Test
		void shouldHandleInterfaceBusinessException() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/interface-business-exception"))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.INTERFACE_ERROR),
						jsonPath(JSON_PATH_MSG).value("External API returned business error"),
						jsonPath(JSON_PATH_DATA).doesNotExist());
		}

	}

	@Nested
	class WhenValidationErrorOccurs {

		@Test
		void shouldHandleMissingRequestParameter() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/required-param"))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.PARAM_INVALID)));
		}

		@Test
		void shouldHandleMethodArgumentNotValid() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc
				.perform(post("/test/validate-body").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.PARAM_INVALID)),
						jsonPath(JSON_PATH_DATA).isArray(), jsonPath(JSON_PATH_DATA + "[0].field").value(FIELD_NAME));
		}

		@Test
		void shouldHandleNativeRequestParamValidation() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/validate-param").param(FIELD_NAME, ""))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.PARAM_INVALID)),
						jsonPath(JSON_PATH_DATA).isArray(), jsonPath(JSON_PATH_DATA + "[0].field").value(FIELD_NAME));
		}

		@Test
		void shouldHandleAopRequestParamValidation() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/aop/validate-param").param(FIELD_NAME, ""))
				.andExpect(status().isBadRequest())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.PARAM_INVALID),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.PARAM_INVALID)),
						jsonPath(JSON_PATH_DATA).isArray(), jsonPath(JSON_PATH_DATA + "[0].field").value(FIELD_NAME));
		}

	}

	@Nested
	class WhenGenericExceptionOccurs {

		@Test
		void shouldHandleGenericException() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc.perform(get("/test/generic-exception"))
				.andExpect(status().isInternalServerError())
				.andExpectAll(jsonPath(JSON_PATH_CODE).value(ErrorCode.SYSTEM_ERROR),
						jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR)));
		}

	}

	@Nested
	class WhenInternationalizationIsEnabled {

		@Test
		void shouldSupportChineseViaAcceptLanguageHeader() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc
				.perform(get(URL_BUSINESS_EXCEPTION).header(HEADER_ACCEPT_LANGUAGE, "zh-CN"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR, Locale.CHINA)));
		}

		@Test
		void shouldSupportEnglishViaAcceptLanguageHeader() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc
				.perform(get(URL_BUSINESS_EXCEPTION).header(HEADER_ACCEPT_LANGUAGE, "en-US"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR, Locale.US)));
		}

		@Test
		void shouldPrioritizeQueryParameterOverAcceptLanguageHeader() throws Exception {
			GlobalExceptionAdviceTests.this.mockMvc
				.perform(get(URL_BUSINESS_EXCEPTION).header(HEADER_ACCEPT_LANGUAGE, "en-US").param("locale", "zh-CN"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath(JSON_PATH_MSG).value(getMessage(ErrorCode.SYSTEM_ERROR, Locale.CHINA)));
		}

	}

	/**
	 * 普通 Controller, 不带 @Validated 注解. 用于测试 Spring MVC 原生验证 (Spring Boot 4.0+ / Spring
	 * 6.1+ 标准).
	 */
	@RestController
	static class NativeValidationController {

		@GetMapping("/test/business-exception")
		void throwBusinessException() {
			throw new BusinessException(ErrorCode.SYSTEM_ERROR);
		}

		@GetMapping("/test/interface-exception")
		void throwInterfaceException() {
			throw new InterfaceException(ErrorCode.INTERFACE_ERROR, null);
		}

		@GetMapping("/test/interface-exception-with-cause")
		void throwInterfaceExceptionWithCause() {
			throw new InterfaceException(ErrorCode.INTERFACE_ERROR, new RuntimeException("Network timeout"));
		}

		@GetMapping("/test/interface-business-exception")
		void throwInterfaceBusinessException() {
			throw new InterfaceBusinessException("External API returned business error");
		}

		@GetMapping("/test/required-param")
		void requireParameter(@RequestParam String name) {
		}

		@PostMapping("/test/validate-body")
		void validateRequestBody(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/test/validate-param")
		void validateRequestParam(@RequestParam @NotBlank(message = "Name cannot be blank") String name) {
		}

		@GetMapping("/test/generic-exception")
		void throwGenericException() {
			throw new RuntimeException("Unexpected error");
		}

	}

	/**
	 * 带有 @Validated 注解的 Controller. 用于测试 AOP 代理触发的验证 (兼容性测试).
	 */
	@Validated
	@RestController
	@RequestMapping("/test/aop")
	static class AopValidatedController {

		@GetMapping("/validate-param")
		void validateRequestParam(@RequestParam @NotBlank(message = "Name cannot be blank") String name) {
		}

	}

	@Data
	static class ValidationRequest {

		@NotBlank(message = "Name is required")
		private String name;

	}

}
