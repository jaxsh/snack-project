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
import lombok.Setter;

/**
 * API响应的字段错误信息. 用于封装参数校验失败时的字段名和错误信息.
 *
 * @author Jax Jiang
 * @since 2025-05-30
 */
@Getter
@Setter
@AllArgsConstructor
public class ApiResponseFieldError {

	/**
	 * 字段名.
	 */
	private String field;

	/**
	 * 错误信息.
	 */
	private String message;

}
