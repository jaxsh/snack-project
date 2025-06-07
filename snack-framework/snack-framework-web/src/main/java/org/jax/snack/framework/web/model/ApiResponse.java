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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.http.HttpStatus;

/**
 * 通用API响应封装类. 用于向客户端返回标准化的响应结构.
 *
 * @param <T> 响应数据的类型.
 * @author Jax Jiang
 * @since 2025-05-29
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

	/**
	 * 状态码. 成功返回OK, 其他返回错误代码.
	 */
	private String code;

	/**
	 * 响应消息. 用于提供操作结果的描述信息, 成功返回"Success", 其他返回错误信息.
	 */
	private String msg;

	/**
	 * 响应数据. 泛型类型, 可以是任何类型的业务数据.
	 */
	private T data;

	/**
	 * 创建一个表示成功的API响应. 状态码默认为 {@link HttpStatus#OK} (200), 消息默认为 "Success".
	 * @param data 成功的响应数据
	 * @param <T> 响应数据的类型
	 * @return 包含成功数据的 {@link ApiResponse} 实例
	 */
	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(String.valueOf(HttpStatus.OK.value()), "Success", data);
	}

	/**
	 * 创建一个表示错误的API响应. 响应数据将为 {@code null}.
	 * @param code 错误状态码
	 * @param msg 错误消息
	 * @param <T> 响应数据的类型 (通常在这种情况下不重要, 因为数据为null)
	 * @return 表示错误的 {@link ApiResponse} 实例
	 */
	public static <T> ApiResponse<T> error(String code, String msg) {
		return new ApiResponse<>(code, msg, null);
	}

	/**
	 * 创建一个表示错误的API响应, 并包含错误数据.
	 * @param code 错误状态码
	 * @param msg 错误消息
	 * @param data 错误数据
	 * @param <T> 响应数据的类型
	 * @return 表示错误的 {@link ApiResponse} 实例
	 */
	public static <T> ApiResponse<T> error(String code, String msg, T data) {
		return new ApiResponse<>(code, msg, data);
	}

}
