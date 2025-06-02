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

import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.web.constant.ErrorCode;
import org.jax.snack.framework.web.exception.BusinessException;
import org.jax.snack.framework.web.exception.InterfaceException;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jax.snack.framework.web.model.ApiResponseFieldError;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 全局异常处理器. 统一处理系统中的各种异常并转换为标准响应格式. 支持处理未知错误、业务错误、外部接口错误、参数校验错误等.
 *
 * @author Jax Jiang
 * @since 2025-05-31
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionAdvice {

	private final MessageSource messageSource;

	/**
	 * 处理未知错误.
	 * @param e 错误信息
	 * @param request http请求
	 * @return 返回报文
	 */
	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ApiResponse<Object> handleException(Exception e, HttpServletRequest request) {
		log.error("Unhandled Exception: Path - {}", request.getRequestURI(), e);
		return ApiResponse.error(ErrorCode.SYSTEM_ERROR.getCode(),
				this.messageSource.getMessage(ErrorCode.SYSTEM_ERROR.getCode(), null, LocaleContextHolder.getLocale()),
				e.getMessage());
	}

	/**
	 * 处理业务错误.
	 * @param e 错误信息
	 * @return 返回报文
	 */
	@ExceptionHandler(BusinessException.class)
	public ApiResponse<Object> handleBusinessException(BusinessException e) {
		return ApiResponse.error(e.getErrorCode().getCode(), this.messageSource.getMessage(e.getErrorCode().getCode(),
				e.getMessageArgs(), LocaleContextHolder.getLocale()));
	}

	/**
	 * 处理外部接口错误.
	 * @param e 错误信息
	 * @param request http请求
	 * @return 返回报文
	 */
	@ExceptionHandler(InterfaceException.class)
	public ApiResponse<Object> handleInterfaceException(InterfaceException e, HttpServletRequest request) {
		log.error("Interface Exception: Path - {}", request.getRequestURI(), e);
		return ApiResponse.error(ErrorCode.INTERFACE_ERROR.getCode(), this.messageSource
			.getMessage(ErrorCode.INTERFACE_ERROR.getCode(), null, LocaleContextHolder.getLocale()), e.getMessage());
	}

	/**
	 * 处理@RequestParam发生的校验错误.
	 * @param e 错误信息
	 * @return 返回报文
	 */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Object> handleMissingServletRequestParameterException(
			MissingServletRequestParameterException e) {
		return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(),
				this.messageSource.getMessage(ErrorCode.PARAM_INVALID.getCode(), null, LocaleContextHolder.getLocale()),
				e.getBody().getDetail());
	}

	/**
	 * 处理@RequestParam(required = false)发生的校验错误.
	 * @param e 参数校验异常
	 * @return 返回报文
	 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Object> handlerHandlerMethodValidationException(HandlerMethodValidationException e) {
		List<ApiResponseFieldError> apiResponseFieldErrors = e.getParameterValidationResults()
			.stream()
			.flatMap((result) -> {
				String fieldName = result.getMethodParameter().getParameterName();
				return result.getResolvableErrors()
					.stream()
					.map((error) -> new ApiResponseFieldError(fieldName, error.getDefaultMessage()));
			})
			.collect(Collectors.toList());

		return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(),
				this.messageSource.getMessage(ErrorCode.PARAM_INVALID.getCode(), null, LocaleContextHolder.getLocale()),
				apiResponseFieldErrors);
	}

	/**
	 * 处理@RequestBody/@ModelAttribute发生的校验错误.
	 * @param e 参数校验异常
	 * @return 返回报文
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ApiResponse<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

		List<ApiResponseFieldError> apiResponseFieldErrors = e.getFieldErrors()
			.stream()
			.map((fieldError) -> new ApiResponseFieldError(fieldError.getField(), fieldError.getDefaultMessage()))
			.collect(Collectors.toList());

		return ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(),
				this.messageSource.getMessage(ErrorCode.PARAM_INVALID.getCode(), null, LocaleContextHolder.getLocale()),
				apiResponseFieldErrors);
	}

}
