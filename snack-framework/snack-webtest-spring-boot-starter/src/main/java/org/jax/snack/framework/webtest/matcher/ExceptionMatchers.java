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

import org.hamcrest.Matchers;

import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 异常响应的快捷断言工具.
 * <p>
 * 提供针对错误响应结构的便捷断言方法, 特别是字段校验错误.
 *
 * @author Jax Jiang
 */
public final class ExceptionMatchers {

	private static final String PATH_CODE = "$.code";

	private static final String PATH_DATA = "$.data";

	private static final String PATH_MSG = "$.msg";

	private ExceptionMatchers() {
	}

	/**
	 * 断言错误码.
	 * @param expectedCode 期望的错误码
	 * @return ResultMatcher
	 */
	public static ResultMatcher code(String expectedCode) {
		return jsonPath(PATH_CODE).value(expectedCode);
	}

	/**
	 * 断言存在字段错误列表.
	 * @return ResultMatcher
	 */
	public static ResultMatcher hasFieldErrors() {
		return jsonPath(PATH_DATA).isArray();
	}

	/**
	 * 断言字段错误数量.
	 * @param expectedCount 期望的错误数量
	 * @return ResultMatcher
	 */
	public static ResultMatcher fieldErrorCount(int expectedCount) {
		return jsonPath(PATH_DATA + ".length()").value(expectedCount);
	}

	/**
	 * 断言包含指定的字段错误.
	 * @param field 字段名
	 * @param message 错误消息
	 * @return ResultMatcher 数组
	 */
	public static ResultMatcher[] fieldError(String field, String message) {
		return new ResultMatcher[] { jsonPath(PATH_DATA + "[?(@.field == '" + field + "')]").exists(),
				jsonPath(PATH_DATA + "[?(@.field == '" + field + "')].message", Matchers.hasItem(message)) };
	}

	/**
	 * 断言包含指定字段的错误 (不验证具体消息).
	 * @param field 字段名
	 * @return ResultMatcher
	 */
	public static ResultMatcher fieldHasError(String field) {
		return jsonPath(PATH_DATA + "[?(@.field == '" + field + "')]").exists();
	}

	/**
	 * 断言错误消息.
	 * @param expectedMsg 期望的错误消息
	 * @return ResultMatcher
	 */
	public static ResultMatcher msg(String expectedMsg) {
		return jsonPath(PATH_MSG).value(expectedMsg);
	}

	/**
	 * 断言错误消息存在.
	 * @return ResultMatcher
	 */
	public static ResultMatcher msgExists() {
		return jsonPath(PATH_MSG).exists();
	}

}
