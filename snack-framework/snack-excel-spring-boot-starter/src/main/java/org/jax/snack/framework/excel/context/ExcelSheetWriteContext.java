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

package org.jax.snack.framework.excel.context;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.excel.config.ExcelSheetExportConfig;
import org.jax.snack.framework.excel.merge.ColumnMergeStrategy;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Excel 写入上下文 (Sheet 级).
 *
 * @param <T> 数据类型
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class ExcelSheetWriteContext<T> {

	private final ExcelWriterSheetBuilder sheetBuilder;

	@Getter
	private List<T> sheetData;

	@Getter
	private Map<String, List<String>> sheetHeaders;

	/**
	 * 配置 Sheet 构建器 (原生).
	 * @param consumer 配置回调
	 * @return this
	 */
	public ExcelSheetWriteContext<T> customize(Consumer<ExcelWriterSheetBuilder> consumer) {
		consumer.accept(this.sheetBuilder);
		return this;
	}

	/**
	 * 设置 Sheet 数据.
	 * @param data 数据列表
	 * @return this
	 */
	public ExcelSheetWriteContext<T> data(List<T> data) {
		this.sheetData = data;
		return this;
	}

	/**
	 * 设置字段到表头的映射.
	 * @param headers 字段到表头列表的映射
	 * @return this
	 */
	public ExcelSheetWriteContext<T> headers(Map<String, List<String>> headers) {
		this.sheetHeaders = headers;
		return this;
	}

	/**
	 * 从配置对象应用设置.
	 * @param config 导出配置
	 * @return this
	 */
	public ExcelSheetWriteContext<T> applyConfig(ExcelSheetExportConfig config) {
		if (config == null) {
			return this;
		}

		if (!CollectionUtils.isEmpty(config.getHeaders())) {
			this.sheetHeaders = config.getHeaders();
		}

		if (Boolean.TRUE.equals(config.getAutoColumnWidth())) {
			this.sheetBuilder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
		}

		if (!ObjectUtils.isEmpty(config.getMergeColumnIndexes())) {
			this.sheetBuilder.registerWriteHandler(new ColumnMergeStrategy(config.getMergeColumnIndexes()));
		}

		return this;
	}

}
