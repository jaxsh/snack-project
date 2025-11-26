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

import org.jax.snack.framework.web.constant.ErrorCode;
import org.jax.snack.framework.web.exception.InterfaceException;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * 默认状态处理器测试.
 * <p>
 * 验证 {@link DefaultStatusHandler} 是否能正确判断 HTTP 状态码是否需要处理, 并在需要处理时抛出
 * {@link InterfaceException}.
 *
 * @author Jax Jiang
 */
class DefaultStatusHandlerTests {

	private final DefaultStatusHandler handler = new DefaultStatusHandler();

	@Test
	void testShouldReturnFalseFor2xx() {
		assertThat(this.handler.test(HttpStatus.OK)).isFalse();
		assertThat(this.handler.test(HttpStatus.CREATED)).isFalse();
	}

	@Test
	void testShouldReturnFalseFor500() {
		assertThat(this.handler.test(HttpStatus.INTERNAL_SERVER_ERROR)).isFalse();
	}

	@Test
	void testShouldReturnTrueForOtherErrors() {
		assertThat(this.handler.test(HttpStatus.BAD_REQUEST)).isTrue();
		assertThat(this.handler.test(HttpStatus.NOT_FOUND)).isTrue();
		assertThat(this.handler.test(HttpStatus.BAD_GATEWAY)).isTrue();
	}

	@Test

	void handleShouldThrowInterfaceException() {

		HttpRequest request = mock(HttpRequest.class);

		@SuppressWarnings("PMD.CloseResource")

		ClientHttpResponse response = mock(ClientHttpResponse.class);

		assertThatExceptionOfType(InterfaceException.class)

			.isThrownBy(() -> this.handler.handle(request, response))

			.satisfies((ex) -> assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTERFACE_ERROR));

	}

}
