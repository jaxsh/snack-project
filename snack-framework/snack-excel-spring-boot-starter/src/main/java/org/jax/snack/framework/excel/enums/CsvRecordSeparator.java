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
 * CSV 行分隔符枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum CsvRecordSeparator implements BaseEnum<String> {

	/**
	 * Windows 换行符 (\r\n).
	 */
	CRLF(CsvConstant.CRLF, "Windows 换行符"),

	/**
	 * Unix/Linux 换行符 (\n).
	 */
	LF(CsvConstant.LF, "Unix/Linux 换行符"),

	/**
	 * Mac 经典换行符 (\r).
	 */
	CR(CsvConstant.CR, "Mac 经典换行符"),

	/**
	 * Form Feed (\f).
	 */
	FF(CsvConstant.FF, "Form Feed");

	/**
	 * 状态值.
	 */
	private final String code;

	/**
	 * 状态名称.
	 */
	private final String name;

	/**
	 * 根据 code 获取枚举实例.
	 * @param code 状态值
	 * @return CsvRecordSeparator 实例
	 */
	public static CsvRecordSeparator of(String code) {
		return BaseEnum.fromCode(CsvRecordSeparator.class, code);
	}

}
