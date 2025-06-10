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

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

/**
 * 默认错误包装拦截器实现. 用于拦截HTTP请求过程中的异常, 并将其转换为统一的接口异常格式.
 *
 * @author Jax Jiang
 * @since 2025-06-08
 */
public class DefaultErrorWrappingInterceptor implements ErrorWrappingInterceptor {

	/**
	 * 拦截HTTP请求, 处理可能发生的异常.
	 * @param request http请求
	 * @param body 请求体
	 * @param execution 请求执行器
	 * @return http响应
	 * @throws InterfaceException 当请求执行过程中发生异常时
	 */
	@Override
	public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body,
			@NonNull ClientHttpRequestExecution execution) {
		try {
			return execution.execute(request, body);
		}
		catch (Exception ex) {
			throw new InterfaceException(ErrorCode.INTERFACE_ERROR, ex);
		}
	}

}
