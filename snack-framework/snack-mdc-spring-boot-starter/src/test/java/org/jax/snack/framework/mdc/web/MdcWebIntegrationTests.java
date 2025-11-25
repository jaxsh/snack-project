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

package org.jax.snack.framework.mdc.web;

import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 测试 Web 请求中 traceId 的处理功能及其配置变体.
 * <p>
 * 包含: 1. 默认行为: 请求头复用, 自动生成, 响应头回显. 2. 排除路径测试. 3. 配置变体: 禁用响应头回显, 自定义响应头名称.
 *
 * @author Jax Jiang
 */
class MdcWebIntegrationTests {

	private static final String ENDPOINT = "/test/endpoint";

	private static final String HEADER_NAME = "X-Trace-Id";

	@Test
	void suiteContextLoads() {
	}

	/**
	 * 测试默认配置下的 Web 行为.
	 */
	@Nested
	@SpringBootTest(classes = WebIntegrationConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
	@AutoConfigureMockMvc
	class DefaultConfigurationTest {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private TraceIdGenerator traceIdGenerator;

		@AfterEach
		void tearDown() {
			MDC.clear();
		}

		@Test
		void shouldReuseTraceIdFromRequestHeader() throws Exception {
			String clientTraceId = "client-trace-12345";

			this.mockMvc.perform(get(ENDPOINT).header(HEADER_NAME, clientTraceId))
				.andExpect(status().isOk())
				.andExpect(header().exists(HEADER_NAME))
				.andExpect(header().string(HEADER_NAME, clientTraceId));
		}

		@Test
		void shouldGenerateTraceIdWhenHeaderMissing() throws Exception {
			String generatedTraceId = "generated-trace-67890";
			given(this.traceIdGenerator.generate()).willReturn(generatedTraceId);

			this.mockMvc.perform(get(ENDPOINT))
				.andExpect(status().isOk())
				.andExpect(header().exists(HEADER_NAME))
				.andExpect(header().string(HEADER_NAME, generatedTraceId));
		}

		@Test
		void shouldExcludeHealthEndpoint() throws Exception {
			this.mockMvc.perform(get("/health"))
				.andExpect(status().isNotFound())
				.andExpect(header().doesNotExist(HEADER_NAME));
		}

		@Test
		void shouldExcludeActuatorEndpoints() throws Exception {
			this.mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isNotFound())
				.andExpect(header().doesNotExist(HEADER_NAME));
		}

	}

	/**
	 * 测试禁用响应头回显的场景. 配置: logging.mdc.include-in-response=false
	 */
	@Nested
	@SpringBootTest(classes = WebIntegrationConfig.class, properties = { "logging.mdc.include-in-response=false" })
	@AutoConfigureMockMvc
	class DisableResponseHeaderTest {

		@Autowired
		private MockMvc mockMvc;

		@Test
		void shouldNotIncludeTraceIdInResponseWhenDisabled() throws Exception {
			this.mockMvc.perform(get(ENDPOINT))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HEADER_NAME));
		}

	}

	/**
	 * 测试自定义响应头名称的场景. 配置: logging.mdc.response-header-name=X-Custom-Trace-Id
	 */
	@Nested
	@SpringBootTest(classes = WebIntegrationConfig.class,
			properties = { "logging.mdc.response-header-name=X-Custom-Trace-Id" })
	@AutoConfigureMockMvc
	class CustomResponseHeaderTest {

		@Autowired
		private MockMvc mockMvc;

		@Autowired
		private TraceIdGenerator traceIdGenerator;

		@Test
		void shouldUrlCustomResponseHeaderName() throws Exception {
			given(this.traceIdGenerator.generate()).willReturn("custom-trace-id-val");

			this.mockMvc.perform(get(ENDPOINT))
				.andExpect(status().isOk())
				.andExpect(header().exists("X-Custom-Trace-Id"))
				.andExpect(header().string("X-Custom-Trace-Id", "custom-trace-id-val"))
				.andExpect(header().doesNotExist(HEADER_NAME));
		}

	}

	@Configuration
	@EnableAutoConfiguration
	static class WebIntegrationConfig {

		@Bean
		MockEndpointController testController() {
			return new MockEndpointController();
		}

		@Bean
		TraceIdGenerator traceIdGenerator() {
			return mock(TraceIdGenerator.class);
		}

	}

	@org.springframework.web.bind.annotation.RestController
	static class MockEndpointController {

		@org.springframework.web.bind.annotation.GetMapping(ENDPOINT)
		String testEndpoint() {
			return "ok";
		}

	}

}
