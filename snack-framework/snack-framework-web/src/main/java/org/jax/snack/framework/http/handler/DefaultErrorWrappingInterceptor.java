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
 * TODO.
 *
 * @author Jax Jiang
 * @since 2025-06-08
 */
public class DefaultErrorWrappingInterceptor implements ErrorWrappingInterceptor {

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
