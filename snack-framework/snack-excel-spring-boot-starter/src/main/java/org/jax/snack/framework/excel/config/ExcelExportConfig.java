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

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Excel 导出配置 (Workbook 级).
 * <p>
 * 支持从外部 (如前端 JSON) 注入导出参数. 包含 Workbook 全局配置和多个 Sheet 配置.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Accessors(chain = true)
public class ExcelExportConfig {

	/**
	 * 是否使用默认样式.
	 * <p>
	 * 默认 true，会自动应用 DEFAULT 样式和自动列宽.
	 */
	private boolean useDefaultStyle = true;

	/**
	 * Excel 打开密码.
	 * <p>
	 * 如果设置，生成的文件将被加密.
	 */
	private String password;

	/**
	 * Sheet 配置列表.
	 */
	private List<ExcelSheetExportConfig> sheets = new ArrayList<>();

	/**
	 * 添加一个 Sheet 配置.
	 * @param sheet Sheet 配置
	 * @return this
	 */
	public ExcelExportConfig addSheet(ExcelSheetExportConfig sheet) {
		this.sheets.add(sheet);
		return this;
	}

}
