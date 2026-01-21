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
import org.jax.snack.framework.core.enums.BaseEnum;

/**
 * CSV 引号枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum CsvQuote implements BaseEnum<Character> {

	/**
	 * 空格.
	 */
	SPACE(CsvConstant.SPACE, "空格"),

	/**
	 * 反斜杠.
	 */
	BACKSLASH(CsvConstant.BACKSLASH, "反斜杠"),

	/**
	 * 退格.
	 */
	BACKSPACE(CsvConstant.BACKSPACE, "退格"),

	/**
	 * 管道符.
	 */
	PIPE(CsvConstant.PIPE, "管道符"),

	/**
	 * 双引号.
	 */
	DOUBLE_QUOTE(CsvConstant.DOUBLE_QUOTE, "双引号");

	/**
	 * 状态值.
	 */
	private final Character code;

	/**
	 * 状态名称.
	 */
	private final String name;

	/**
	 * 根据 code 获取枚举实例.
	 * @param code 状态值
	 * @return CsvQuote 实例
	 */
	public static CsvQuote of(Character code) {
		return BaseEnum.fromCode(CsvQuote.class, code);
	}

}
