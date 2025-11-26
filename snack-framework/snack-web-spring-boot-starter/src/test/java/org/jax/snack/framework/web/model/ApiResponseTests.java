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

package org.jax.snack.framework.web.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * API 响应模型测试.
 * <p>
 * 验证 {@link ApiResponse} 的静态工厂方法是否能正确创建包含成功或失败状态及数据的响应对象.
 *
 * @author Jax Jiang
 */
class ApiResponseTests {

	@Test
	void successShouldReturnOkResponse() {
		String data = "test data";
		ApiResponse<String> response = ApiResponse.success(data);

		assertThat(response.getCode()).isEqualTo("200");
		assertThat(response.getMsg()).isEqualTo("Success");
		assertThat(response.getData()).isEqualTo(data);
	}

	@Test
	void errorShouldReturnErrorResponseWithoutData() {
		String code = "500";
		String msg = "Error";
		ApiResponse<Object> response = ApiResponse.error(code, msg);

		assertThat(response.getCode()).isEqualTo(code);
		assertThat(response.getMsg()).isEqualTo(msg);
		assertThat(response.getData()).isNull();
	}

	@Test
	void errorShouldReturnErrorResponseWithData() {
		String code = "400";
		String msg = "Bad Request";
		String data = "Details";
		ApiResponse<String> response = ApiResponse.error(code, msg, data);

		assertThat(response.getCode()).isEqualTo(code);
		assertThat(response.getMsg()).isEqualTo(msg);
		assertThat(response.getData()).isEqualTo(data);
	}

}
