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
import org.jspecify.annotations.NonNull;

import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

/**
 * 默认HTTP状态码处理器. 用于处理HTTP响应中的非成功状态码, 将其转换为统一的接口异常.
 *
 * @author Jax Jiang
 */
public class DefaultStatusHandler implements CustomResponseErrorHandler {

	/**
	 * 测试是否需要处理该状态码. 对于2xx和500状态码不进行处理.
	 * @param status http状态码
	 * @return 如果需要处理返回true, 否则返回false
	 */
	@Override
	public boolean test(HttpStatusCode status) {
		return !status.is2xxSuccessful() && !status.isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * 处理非2xx和500状态码的响应. 抛出接口异常.
	 * @param request http请求
	 * @param response http响应
	 */
	@Override
	public void handle(@NonNull HttpRequest request, @NonNull ClientHttpResponse response) {
		throw new InterfaceException(ErrorCode.INTERFACE_ERROR, null);
	}

}
