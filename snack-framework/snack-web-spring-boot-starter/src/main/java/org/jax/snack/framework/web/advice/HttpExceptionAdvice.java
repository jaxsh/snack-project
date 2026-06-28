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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.model.ApiResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * HTTP 层异常处理器. 处理 DispatcherServlet 在进入 Controller 前抛出的 HTTP 层异常, 确保这些异常以统一的 ApiResponse
 * 格式返回, 而不是 Spring Boot 默认的 whitelabel 格式.
 *
 * @author Jax Jiang
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@RestControllerAdvice
public class HttpExceptionAdvice {

	private final MessageSource messageSource;

	/**
	 * 处理 404 资源不存在.
	 * @return 返回报文
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ApiResponse<Object> handleNotFound() {
		return ApiResponse.error(ErrorCode.NOT_FOUND, getLocalizedMessage());
	}

	/**
	 * 处理 405 请求方法不支持.
	 * @param e 异常信息
	 * @return 返回报文
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public ApiResponse<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return ApiResponse.error(ErrorCode.PARAM_INVALID, e.getMessage());
	}

	/**
	 * 处理 415 媒体类型不支持.
	 * @param e 异常信息
	 * @return 返回报文
	 */
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
	public ApiResponse<Object> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
		return ApiResponse.error(ErrorCode.PARAM_INVALID, e.getMessage());
	}

	private String getLocalizedMessage() {
		return this.messageSource.getMessage(ErrorCode.NOT_FOUND, null, LocaleContextHolder.getLocale());
	}

}
