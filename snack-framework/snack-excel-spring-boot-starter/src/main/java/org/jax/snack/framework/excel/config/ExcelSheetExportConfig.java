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

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Sheet 导出配置.
 * <p>
 * 支持从外部 (如前端 JSON) 注入导出参数.
 * <p>
 * 表头优先级: headers > headClass
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelSheetExportConfig {

	/**
	 * Sheet 名称.
	 */
	private String sheetName = "Sheet1";

	/**
	 * 是否自动列宽.
	 * <p>
	 * true: 开启自动列宽 (LongestMatchColumnWidthStyleStrategy).
	 */
	private Boolean autoColumnWidth;

	/**
	 * 需要合并的列索引数组.
	 * <p>
	 * 指定哪些列需要进行基于内容的自动合并 (ColumnMergeStrategy).
	 */
	private int[] mergeColumnIndexes;

	/**
	 * 字段到表头的映射 (Key: 字段名, Value: 表头文本列表).
	 * <p>
	 * 单行表头示例: {"id": ["编号"], "name": ["名称"]}
	 * <p>
	 * 多行表头示例: <pre>
	 * {
	 *   "dictType": ["分组", "字典类型"],
	 *   "dictLabel": ["分组", "字典标签"]
	 * }
	 * </pre> 相同的第一行文本会自动合并单元格.
	 */
	private Map<String, List<String>> headers;

}
