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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.excel.config.ExcelExportConfig;
import org.jax.snack.framework.excel.config.ExcelSheetExportConfig;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Excel 写入上下文 (Workbook 级).
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class ExcelWriteContext {

	@Getter
	private final ExcelWriterBuilder writerBuilder;

	@Getter
	private final List<Consumer<ExcelWriter>> sheetTasks = new ArrayList<>();

	@Getter
	private boolean useDefaultStyle = true;

	/**
	 * 配置 Workbook 构建器 (原生).
	 * @param consumer 配置回调
	 * @return this
	 */
	public ExcelWriteContext customize(Consumer<ExcelWriterBuilder> consumer) {
		consumer.accept(this.writerBuilder);
		return this;
	}

	/**
	 * 禁用全局默认样式.
	 * <p>
	 * 调用此方法后，框架将不会自动注册默认样式(DEFAULT)和自动列宽策略. 适用于需要完全自定义样式（如 Sheet 级独立样式）的场景.
	 * @return this
	 */
	public ExcelWriteContext ignoreDefaultStyle() {
		this.useDefaultStyle = false;
		return this;
	}

	/**
	 * 设置表格样式 (快捷方式).
	 * <p>
	 * 调用此方法会自动禁用默认样式，并应用指定的样式.
	 * @param style 样式策略
	 * @return this
	 */
	public ExcelWriteContext style(HorizontalCellStyleStrategy style) {
		this.useDefaultStyle = false;
		this.writerBuilder.registerWriteHandler(style);
		return this;
	}

	/**
	 * 批量应用导出配置 (一次性配置所有 Sheet).
	 * <p>
	 * 根据 ExcelExportConfig 中的配置，自动创建所有 Sheet.
	 * @param config 导出配置
	 * @param dataMap 数据映射 (Key: sheetName, Value: 数据列表)
	 * @param headClass 表头类型
	 * @param <T> 数据类型
	 * @return this
	 */
	public <T> ExcelWriteContext applyConfig(ExcelExportConfig config, Map<String, List<T>> dataMap,
			Class<T> headClass) {
		if (config == null) {
			return this;
		}

		if (!config.isUseDefaultStyle()) {
			this.useDefaultStyle = false;
		}
		if (StringUtils.hasText(config.getPassword())) {
			this.writerBuilder.password(config.getPassword());
		}

		if (!ObjectUtils.isEmpty(config.getSheets())) {
			for (ExcelSheetExportConfig sheetConfig : config.getSheets()) {
				List<T> data = (dataMap != null) ? dataMap.get(sheetConfig.getSheetName()) : null;
				sheet(sheetConfig.getSheetName(), headClass, (sheetCtx) -> {
					sheetCtx.applyConfig(sheetConfig);
					if (data != null) {
						sheetCtx.data(data);
					}
				});
			}
		}
		return this;
	}

	/**
	 * 添加一个 Sheet (使用默认配置).
	 * <p>
	 * 使用默认 Sheet 名 ("Sheet1").
	 * @param headClass 表头类型
	 * @param consumer Sheet 配置回调
	 * @param <T> 数据类型
	 * @return this
	 */
	public <T> ExcelWriteContext sheet(Class<T> headClass, Consumer<ExcelSheetWriteContext<T>> consumer) {
		String sheetName = "Sheet1";
		return sheet(sheetName, headClass, consumer);
	}

	/**
	 * 添加一个 Sheet.
	 * @param sheetName Sheet 名称
	 * @param headClass 表头类型
	 * @param consumer Sheet 配置回调
	 * @param <T> 数据类型
	 * @return this
	 */
	public <T> ExcelWriteContext sheet(String sheetName, Class<T> headClass,
			Consumer<ExcelSheetWriteContext<T>> consumer) {
		this.sheetTasks.add((writer) -> {
			ExcelWriterSheetBuilder sheetBuilder = EasyExcel.writerSheet(sheetName);
			ExcelSheetWriteContext<T> sheetContext = new ExcelSheetWriteContext<>(sheetBuilder);

			consumer.accept(sheetContext);

			Map<String, List<String>> headers = sheetContext.getSheetHeaders();
			if (!CollectionUtils.isEmpty(headers)) {
				List<String> fieldNames = new ArrayList<>(headers.size());
				List<List<String>> headList = new ArrayList<>(headers.size());

				headers.forEach((field, head) -> {
					fieldNames.add(field);
					headList.add(head);
				});

				sheetBuilder.includeColumnFieldNames(fieldNames);
				sheetBuilder.head(headList);
			}
			else {
				sheetBuilder.head(headClass);
			}

			writer.write(sheetContext.getSheetData(), sheetBuilder.build());
		});
		return this;
	}

	/**
	 * 添加一个 Sheet (使用默认配置).
	 * @param sheetName Sheet 名称
	 * @param data 数据
	 * @param headClass 表头类型
	 * @param <T> 数据类型
	 * @return this
	 */
	public <T> ExcelWriteContext sheet(String sheetName, List<T> data, Class<T> headClass) {
		return sheet(sheetName, headClass, (ctx) -> ctx.data(data));
	}

	/**
	 * 添加一个 Sheet (动态表头).
	 * @param sheetName Sheet 名称
	 * @param consumer Sheet 配置回调
	 * @param <T> 数据类型
	 * @return this
	 */
	public <T> ExcelWriteContext sheet(String sheetName, Consumer<ExcelSheetWriteContext<T>> consumer) {
		return sheet(sheetName, null, consumer);
	}

}
