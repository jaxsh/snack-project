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

package org.jax.snack.framework.http.integration;

import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.http.config.RestClientAutoConfiguration;
import org.jax.snack.framework.http.exception.InterfaceException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * RestClient集成测试. 测试RestClientAutoConfiguration配置的HTTP错误处理组件,
 * 包括DefaultStatusHandler和DefaultErrorWrappingInterceptor与RestClient的集成.
 *
 * @author Jax Jiang
 */
@RestClientTest
@Import(RestClientAutoConfiguration.class)
class RestClientIntegrationTests {

	private static final String BASE_URL = "https://external-api.example.com";

	private static final String API_ENDPOINT = "/api/resource";

	@Autowired
	private RestClient.Builder restClientBuilder;

	@Nested
	class WhenHttpClientErrorOccurs {

		@Test
		void shouldWrap400ErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

			mockServer.verify();
		}

		@Test
		void shouldWrap404ErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond(withStatus(HttpStatus.NOT_FOUND));

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

			mockServer.verify();
		}

		@Test
		void shouldWrap403ErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond(withStatus(HttpStatus.FORBIDDEN));

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

			mockServer.verify();
		}

	}

	@Nested
	class WhenHttpServerErrorOccurs {

		@Test
		void shouldWrap502ErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

			mockServer.verify();
		}

		@Test
		void shouldWrap503ErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT))
				.andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

			mockServer.verify();
		}

	}

	@Nested
	class WhenApiCallSucceeds {

		@Test
		void shouldReturnSuccessfulJsonResponse() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			String expectedResponse = "{\"status\":\"success\"}";
			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT))
				.andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

			String actualResponse = restClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

			assertThat(actualResponse).isEqualTo(expectedResponse);
			mockServer.verify();
		}

		@Test
		void shouldReturnSuccessfulTextResponse() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			String expectedResponse = "Hello World";
			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT))
				.andRespond(withSuccess(expectedResponse, MediaType.TEXT_PLAIN));

			String actualResponse = restClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

			assertThat(actualResponse).isEqualTo(expectedResponse);
			mockServer.verify();
		}

	}

	@Nested
	class WhenSpecialStatusCodesOccur {

		@Test
		void shouldNotWrap500ErrorAsItIsHandledSpecially() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

			assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class));

			mockServer.verify();
		}

		@Test
		void shouldNotWrap2xxSuccessResponses() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT))
				.andRespond(withStatus(HttpStatus.CREATED).body("Created").contentType(MediaType.TEXT_PLAIN));

			String result = restClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

			assertThat(result).isEqualTo("Created");
			mockServer.verify();
		}

	}

	@Nested
	class WhenNetworkErrorOccurs {

		@Test
		void shouldWrapConnectionErrorInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			// 模拟连接错误，使用 SERVER_ERROR 响应并期望抛出异常
			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond((request) -> {
				throw new java.io.IOException("Connection refused");
			});

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> {
					assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR);
					assertThat(ex.getCause()).isInstanceOf(java.io.IOException.class);
				});

			mockServer.verify();
		}

	}

	@Nested
	class WhenTimeoutOccurs {

		@Test
		void shouldWrapSocketTimeoutExceptionInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			// 模拟 SocketTimeoutException (读取或连接超时)
			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond((request) -> {
				throw new java.net.SocketTimeoutException("Read timed out");
			});

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> {
					assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR);
					assertThat(ex.getCause()).isInstanceOf(java.net.SocketTimeoutException.class);
					assertThat(ex.getCause()).hasMessageContaining("timed out");
				});

			mockServer.verify();
		}

		@Test
		void shouldWrapGenericIOExceptionInInterfaceException() {
			MockRestServiceServer mockServer = MockRestServiceServer
				.bindTo(RestClientIntegrationTests.this.restClientBuilder)
				.build();
			RestClient restClient = RestClientIntegrationTests.this.restClientBuilder.baseUrl(BASE_URL).build();

			// 模拟通用 IOException
			mockServer.expect(requestTo(BASE_URL + API_ENDPOINT)).andRespond((request) -> {
				throw new java.io.IOException("Network error occurred");
			});

			assertThatExceptionOfType(InterfaceException.class)
				.isThrownBy(() -> restClient.get().uri(API_ENDPOINT).retrieve().body(String.class))
				.satisfies((ex) -> {
					assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR);
					assertThat(ex.getCause()).isInstanceOf(java.io.IOException.class);
					assertThat(ex.getCause()).hasMessageContaining("Network error");
				});

			mockServer.verify();
		}

	}

	@SpringBootApplication
	static class DummyApplication {

	}

}
