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
 * PageResult 分页结构的快捷断言工具.
 * <p>
 * 提供针对分页响应结构 {@code $.data.records} 的便捷断言方法.
 *
 * @author Jax Jiang
 */
public final class PageResultMatchers {

	private static final String PATH_DATA = "$.data";

	private static final String PATH_RECORDS = PATH_DATA + ".records";

	private static final String PATH_TOTAL = PATH_DATA + ".total";

	private static final String PATH_SIZE = PATH_DATA + ".size";

	private static final String PATH_CURRENT = PATH_DATA + ".current";

	private static final String PATH_PAGES = PATH_DATA + ".pages";

	private PageResultMatchers() {
	}

	/**
	 * 断言总记录数.
	 * @param expected 期望的总记录数
	 * @return ResultMatcher
	 */
	public static ResultMatcher totalIs(long expected) {
		return jsonPath(PATH_TOTAL).value(expected);
	}

	/**
	 * 断言每页大小.
	 * @param expected 期望的每页大小
	 * @return ResultMatcher
	 */
	public static ResultMatcher sizeIs(long expected) {
		return jsonPath(PATH_SIZE).value(expected);
	}

	/**
	 * 断言当前页码.
	 * @param expected 期望的当前页码
	 * @return ResultMatcher
	 */
	public static ResultMatcher currentIs(long expected) {
		return jsonPath(PATH_CURRENT).value(expected);
	}

	/**
	 * 断言总页数.
	 * @param expected 期望的总页数
	 * @return ResultMatcher
	 */
	public static ResultMatcher pagesIs(long expected) {
		return jsonPath(PATH_PAGES).value(expected);
	}

	/**
	 * 断言分页结果为空.
	 * @return ResultMatcher
	 */
	public static ResultMatcher isEmpty() {
		return jsonPath(PATH_RECORDS).isEmpty();
	}

	/**
	 * 断言分页结果不为空.
	 * @return ResultMatcher
	 */
	public static ResultMatcher isNotEmpty() {
		return jsonPath(PATH_RECORDS).isNotEmpty();
	}

	/**
	 * 断言记录数组中指定索引的字段值.
	 * @param index 记录索引 (从0开始)
	 * @param path 相对于记录对象的路径 (如 ".id", ".name")
	 * @param expectedValue 期望的值
	 * @return ResultMatcher
	 */
	public static ResultMatcher record(int index, String path, Object expectedValue) {
		return jsonPath(PATH_RECORDS + "[" + index + "]" + path).value(expectedValue);
	}

	/**
	 * 断言记录数组中指定索引的字段存在.
	 * @param index 记录索引
	 * @param path 相对于记录对象的路径
	 * @return ResultMatcher
	 */
	public static ResultMatcher recordExists(int index, String path) {
		return jsonPath(PATH_RECORDS + "[" + index + "]" + path).exists();
	}

	/**
	 * 断言记录数组的长度.
	 * @param expectedCount 期望的记录数量
	 * @return ResultMatcher
	 */
	public static ResultMatcher recordCount(int expectedCount) {
		return jsonPath(PATH_RECORDS + ".length()").value(expectedCount);
	}

}
