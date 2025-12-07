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

package org.jax.snack.framework.excel.enums;

import cn.idev.excel.metadata.csv.CsvConstant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CSV 分隔符枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum CsvDelimiter {

	/**
	 * 逗号.
	 */
	COMMA(CsvConstant.COMMA.charAt(0)),

	/**
	 * 制表符.
	 */
	TAB(CsvConstant.TAB.charAt(0)),

	/**
	 * At 符号 (@).
	 */
	AT(CsvConstant.AT.charAt(0));

	private final char value;

}
