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

package org.jax.snack.framework.http.handler;

import java.io.IOException;

import org.jax.snack.framework.web.constant.ErrorCode;
import org.jax.snack.framework.web.exception.InterfaceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 默认错误包装拦截器测试.
 * <p>
 * 验证 {@link DefaultErrorWrappingInterceptor} 是否能正确拦截 HTTP 请求, 并在遇到 {@link IOException}
 * 时将其包装为 {@link InterfaceException}.
 *
 * @author Jax Jiang
 */
@ExtendWith(MockitoExtension.class)
class DefaultErrorWrappingInterceptorTests {

	@Mock
	private ClientHttpRequestExecution execution;

	@Mock
	private HttpRequest request;

	private final DefaultErrorWrappingInterceptor interceptor = new DefaultErrorWrappingInterceptor();

	@Test
	void interceptShouldExecuteRequestSuccessfully() throws IOException {
		byte[] body = new byte[0];
		@SuppressWarnings("PMD.CloseResource")
		ClientHttpResponse expectedResponse = mock(ClientHttpResponse.class);
		given(this.execution.execute(this.request, body)).willReturn(expectedResponse);

		@SuppressWarnings("PMD.CloseResource")
		ClientHttpResponse response = this.interceptor.intercept(this.request, body, this.execution);

		assertThat(response).isEqualTo(expectedResponse);
	}

	@Test
	void interceptShouldWrapIOExceptionInInterfaceException() throws IOException {
		byte[] body = new byte[0];
		IOException ioException = new IOException("Network error");
		given(this.execution.execute(this.request, body)).willThrow(ioException);

		assertThatExceptionOfType(InterfaceException.class)
			.isThrownBy(() -> this.interceptor.intercept(this.request, body, this.execution))
			.withMessage(ErrorCode.INTERFACE_ERROR) // Expect the error code as the
													// message
			.satisfies((ex) -> assertThat(ex.getCause()).isEqualTo(ioException));
	}

}
