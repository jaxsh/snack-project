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

package org.jax.snack.framework.web.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 校验配置测试. 验证 Hibernate Validator 的 FailFast 机制.
 *
 * @author Jax Jiang
 */
@WebMvcTest
@Import({ WebAutoConfiguration.class, ValidationConfigurationTests.ValidationController.class })
class ValidationConfigurationTests {

	@Autowired
	private MockMvc mockMvc;

	@Nested
	class WhenFailFastIsEnabled {

		@Test
		void shouldReturnSingleErrorWhenFailFastIsDefaultTrue() throws Exception {
			ValidationConfigurationTests.this.mockMvc
				.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.length()").value(1));
		}

	}

	@Nested
	@TestPropertySource(properties = "spring.mvc.validation.fail-fast=false")
	class WhenFailFastIsDisabled {

		@Test
		void shouldReturnMultipleErrorsWhenFailFastIsDisabled() throws Exception {
			ValidationConfigurationTests.this.mockMvc
				.perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.data.length()").value(2));
		}

	}

	@RestController
	static class ValidationController {

		@PostMapping("/test/validate")
		void validate(@Valid @RequestBody ValidationDto dto) {
		}

	}

	@Data
	static class ValidationDto {

		@NotNull
		private String field1;

		@NotNull
		private String field2;

	}

	@org.springframework.boot.autoconfigure.SpringBootApplication
	static class DummyApplication {

	}

}
