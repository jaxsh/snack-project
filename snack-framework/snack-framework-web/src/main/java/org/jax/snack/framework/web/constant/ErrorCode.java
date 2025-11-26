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

package org.jax.snack.framework.web.constant;

/**
 * 系统中使用的错误代码常量.
 *
 * @author Jax Jiang
 */
public class ErrorCode {

	protected ErrorCode() {

	}

	/**
	 * 系统错误. 表示系统内部发生的未预期错误.
	 */
	public static final String SYSTEM_ERROR = "1000";

	/**
	 * 接口错误. 表示接口调用过程中发生的错误.
	 */
	public static final String INTERFACE_ERROR = "1001";

	/**
	 * 参数无效. 表示请求参数不符合要求.
	 */
	public static final String PARAM_INVALID = "1002";

}
