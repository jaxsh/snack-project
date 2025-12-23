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

package org.jax.snack.framework.webtest.matcher;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * ApiResponse 结构的快捷断言工具.
 * <p>
 * 提供针对统一响应结构 {@code {"code": null, "msg": "Success", "data": ...}} 的便捷断言方法.
 *
 * @author Jax Jiang
 */
public final class ApiResponseMatchers {

	private static final String PATH_CODE = "$.code";

	private static final String PATH_MSG = "$.msg";

	private static final String PATH_DATA = "$.data";

	private static final String PATH_SUCCESS = "$.success";

	private static final String SUCCESS_MSG = "Success";

	private ApiResponseMatchers() {
	}

	/**
	 * 断言响应为成功状态.
	 * <p>
	 * 验证 code 为 null, msg 为 "Success", success 为 true.
	 * @return ResultMatcher 数组
	 */
	public static ResultMatcher[] isSuccess() {
		return new ResultMatcher[] { jsonPath(PATH_CODE).doesNotExist(), jsonPath(PATH_MSG).value(SUCCESS_MSG),
				jsonPath(PATH_SUCCESS).value(true) };
	}

	/**
	 * 断言响应为错误状态.
	 * @param expectedCode 期望的错误码
	 * @return ResultMatcher
	 */
	public static ResultMatcher isError(String expectedCode) {
		return jsonPath(PATH_CODE).value(expectedCode);
	}

	/**
	 * 断言响应为错误状态并验证消息.
	 * @param expectedCode 期望的错误码
	 * @param expectedMsg 期望的错误消息
	 * @return ResultMatcher 数组
	 */
	public static ResultMatcher[] isError(String expectedCode, String expectedMsg) {
		return new ResultMatcher[] { jsonPath(PATH_CODE).value(expectedCode), jsonPath(PATH_MSG).value(expectedMsg) };
	}

	/**
	 * 断言 data 字段中指定路径的值.
	 * @param path 相对于 data 的路径 (如 ".id", ".name", "[0].id")
	 * @param expectedValue 期望的值
	 * @return ResultMatcher
	 */
	public static ResultMatcher data(String path, Object expectedValue) {
		return jsonPath(PATH_DATA + path).value(expectedValue);
	}

	/**
	 * 断言 data 字段中指定路径存在.
	 * @param path 相对于 data 的路径
	 * @return ResultMatcher
	 */
	public static ResultMatcher dataExists(String path) {
		return jsonPath(PATH_DATA + path).exists();
	}

	/**
	 * 断言 data 字段中指定路径不存在.
	 * @param path 相对于 data 的路径
	 * @return ResultMatcher
	 */
	public static ResultMatcher dataNotExists(String path) {
		return jsonPath(PATH_DATA + path).doesNotExist();
	}

	/**
	 * 断言 data 字段为数组.
	 * @return ResultMatcher
	 */
	public static ResultMatcher dataIsArray() {
		return jsonPath(PATH_DATA).isArray();
	}

	/**
	 * 断言 data 字段不存在.
	 * @return ResultMatcher
	 */
	public static ResultMatcher dataNotExists() {
		return jsonPath(PATH_DATA).doesNotExist();
	}

	/**
	 * 断言 msg 字段的值.
	 * @param expectedMsg 期望的消息
	 * @return ResultMatcher
	 */
	public static ResultMatcher msg(String expectedMsg) {
		return jsonPath(PATH_MSG).value(expectedMsg);
	}

	/**
	 * 断言 code 字段的值.
	 * @param expectedCode 期望的错误码
	 * @return ResultMatcher
	 */
	public static ResultMatcher code(String expectedCode) {
		return jsonPath(PATH_CODE).value(expectedCode);
	}

}
