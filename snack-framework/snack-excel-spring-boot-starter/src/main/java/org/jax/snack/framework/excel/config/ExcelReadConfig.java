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

package org.jax.snack.framework.excel.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Excel 读取配置.
 * <p>
 * 用于动态控制读取行为 (Sheet 选择、表头行数等).
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelReadConfig {

	/**
	 * 读取的 Sheet 索引 (默认 0).
	 * <p>
	 * 如果同时设置了 sheetName，优先使用 sheetName.
	 */
	private Integer sheetNo;

	/**
	 * 读取的 Sheet 名称.
	 */
	private String sheetName;

	/**
	 * 表头行数 (默认 1).
	 * <p>
	 * 指定数据从第几行开始读取 (索引从 0 开始，1 表示跳过第一行).
	 */
	private Integer headRowNumber;

	/**
	 * 是否忽略空行 (默认 true).
	 */
	private Boolean ignoreEmptyRow;

}
