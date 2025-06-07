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

import java.lang.reflect.Method;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jax.snack.framework.web.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GlobalResponseBodyAdvice 单元测试.
 *
 * @author Jax Jiang
 * @since 2025-05-29
 */
@ExtendWith(MockitoExtension.class)
class GlobalResponseBodyAdviceTests {

	private GlobalResponseBodyAdvice advice;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		this.objectMapper = new ObjectMapper();
		this.advice = new GlobalResponseBodyAdvice(this.objectMapper);
	}

	private Object simulateSpringMvcHandling(Object body, MethodParameter param, MediaType mediaType,
			ServletServerHttpRequest request, ServletServerHttpResponse response) {
		if (this.advice.supports(param, MappingJackson2HttpMessageConverter.class)) {
			return this.advice.beforeBodyWrite(body, param, mediaType, MappingJackson2HttpMessageConverter.class,
					request, response);
		}
		return body;
	}

	@Test
	void testWrapStringHtml() throws NoSuchMethodException {
		Method method = Dummy.class.getDeclaredMethod("stringMethod");
		MethodParameter param = new MethodParameter(method, -1);

		String body = "hello";
		Object result = simulateSpringMvcHandling(body, param, MediaType.TEXT_HTML,
				new ServletServerHttpRequest(new MockHttpServletRequest()),
				new ServletServerHttpResponse(new MockHttpServletResponse()));

		assertThat(result).isEqualTo(body);
	}

	@Test
	void testWrapString() throws NoSuchMethodException, JsonProcessingException {
		Method method = Dummy.class.getDeclaredMethod("stringMethod");
		MethodParameter param = new MethodParameter(method, -1);

		String body = "hello";
		Object result = simulateSpringMvcHandling(body, param, MediaType.APPLICATION_JSON,
				new ServletServerHttpRequest(new MockHttpServletRequest()),
				new ServletServerHttpResponse(new MockHttpServletResponse()));

		ApiResponse<?> apiResponse = this.objectMapper.readValue((String) result, ApiResponse.class);
		assertThat(apiResponse.getCode()).isEqualTo(String.valueOf(HttpStatus.OK.value()));
		assertThat(apiResponse.getMsg()).isEqualTo("Success");
		assertThat(apiResponse.getData()).isEqualTo("hello");
	}

	@Test
	void testWrapMap() throws NoSuchMethodException {
		Method method = Dummy.class.getDeclaredMethod("mapMethod");
		MethodParameter param = new MethodParameter(method, -1);

		Map<String, String> body = Map.of("key", "value");
		Object result = simulateSpringMvcHandling(body, param, MediaType.APPLICATION_JSON,
				new ServletServerHttpRequest(new MockHttpServletRequest()),
				new ServletServerHttpResponse(new MockHttpServletResponse()));

		assertThat(result).isInstanceOf(ApiResponse.class);
		ApiResponse<?> apiResponse = (ApiResponse<?>) result;
		assertThat(apiResponse.getData()).isEqualTo(body);
	}

	@Test
	void testReturnResponseEntity() throws NoSuchMethodException {
		Method method = Dummy.class.getDeclaredMethod("responseEntityMethod");
		MethodParameter param = new MethodParameter(method, -1);

		ResponseEntity<String> body = ResponseEntity.ok("test");
		Object result = simulateSpringMvcHandling(body, param, MediaType.APPLICATION_JSON,
				new ServletServerHttpRequest(new MockHttpServletRequest()),
				new ServletServerHttpResponse(new MockHttpServletResponse()));

		assertThat(result).isEqualTo(body);
	}

	@Test
	void testReturnApiResponse() throws NoSuchMethodException {
		Method method = Dummy.class.getDeclaredMethod("apiResponseMethod");
		MethodParameter param = new MethodParameter(method, -1);

		ApiResponse<String> body = ApiResponse.success("test");
		Object result = simulateSpringMvcHandling(body, param, MediaType.APPLICATION_JSON,
				new ServletServerHttpRequest(new MockHttpServletRequest()),
				new ServletServerHttpResponse(new MockHttpServletResponse()));

		assertThat(result).isEqualTo(body);
	}

	private static class Dummy {

		String stringMethod() {
			return "hello";
		}

		Map<String, String> mapMethod() {
			return Map.of("key", "value");
		}

		ResponseEntity<String> responseEntityMethod() {
			return ResponseEntity.ok("test");
		}

		ApiResponse<String> apiResponseMethod() {
			return ApiResponse.success("test");
		}

	}

}
