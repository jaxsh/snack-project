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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.web.model.ApiResponse;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 全局响应体处理器. 用于统一处理控制器返回的响应体, 将其包装为标准的 {@link ApiResponse} 格式. 主要功能: 1. 对 JSON 类型的响应进行包装
 * 2. 对字符串类型的响应进行特殊处理 3. 保持非 JSON 类型响应的原始格式
 *
 * @author Jax Jiang
 * @since 2025-05-29
 */
@RequiredArgsConstructor
@RestControllerAdvice(annotations = RestController.class)
public class GlobalResponseBodyAdvice implements ResponseBodyAdvice<Object> {

	private final ObjectMapper objectMapper;

	/**
	 * 判断是否需要处理响应体. 对于 {@link ResponseEntity} 和 {@link ApiResponse} 类型的响应不进行处理.
	 * @param returnType 返回类型
	 * @param converterType 消息转换器类型
	 * @return 如果需要处理返回 true, 否则返回 false
	 */
	@Override
	public boolean supports(MethodParameter returnType,
			@NonNull Class<? extends HttpMessageConverter<?>> converterType) {
		return !(returnType.getParameterType().equals(ResponseEntity.class)
				|| returnType.getParameterType().equals(ApiResponse.class));
	}

	/**
	 * 在响应体写入之前进行处理. 对于 JSON 类型的响应: 1. 如果是字符串类型, 需要特殊处理以避免重复序列化 2. 其他类型直接包装为
	 * {@link ApiResponse} 对于非 JSON 类型的响应, 保持原始格式不变.
	 * @param body 响应体
	 * @param returnType 返回类型
	 * @param selectedContentType 选中的内容类型
	 * @param selectedConverterType 选中的转换器类型
	 * @param request 请求
	 * @param response 响应
	 * @return 处理后的响应体
	 */
	@Override
	public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, MediaType selectedContentType,
			@NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType, @NonNull ServerHttpRequest request,
			@NonNull ServerHttpResponse response) {
		if (selectedContentType.equals(MediaType.APPLICATION_JSON)) {
			if (body instanceof String) {
				try {
					return this.objectMapper.writeValueAsString(ApiResponse.success(body));
				}
				catch (JsonProcessingException ex) {
					throw new RuntimeException("Error converting String to JSON", ex);
				}
			}
			return ApiResponse.success(body);
		}
		return body;
	}

}
