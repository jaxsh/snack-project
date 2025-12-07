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

package org.jax.snack.framework.excel;

import java.io.OutputStream;
import java.util.Optional;
import java.util.function.Consumer;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.excel.config.CsvProperties;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.context.CsvWriteContext;
import org.jax.snack.framework.excel.context.ExcelWriteContext;
import org.jax.snack.framework.excel.converter.ZonedDateTimeConverter;
import org.jax.snack.framework.excel.converter.ZonedDateTimeStringConverter;
import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.jax.snack.framework.excel.enums.CsvQuote;
import org.jax.snack.framework.excel.enums.CsvRecordSeparator;
import org.jax.snack.framework.excel.style.ExcelStyleFactory;

/**
 * Excel 写入服务.
 * <p>
 * 提供基于 FastExcel 的 Excel 文件导出功能.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public class ExcelWriteService {

	private final ExcelProperties excelProperties;

	private final CsvProperties csvProperties;

	/**
	 * 创建并初始化 ExcelWriterBuilder.
	 * @param outputStream 输出流
	 * @return Builder
	 */
	public ExcelWriterBuilder createWriterBuilder(OutputStream outputStream) {
		return EasyExcel.write(outputStream)
			.registerConverter(new LongStringConverter())
			.registerConverter(new ZonedDateTimeConverter())
			.orderByIncludeColumn(true);
	}

	/**
	 * 执行 Excel 写入.
	 * @param context 写入上下文
	 */
	public void write(ExcelWriteContext context) {
		ExcelWriterBuilder builder = context.getWriterBuilder();

		// 应用默认样式 (如果未被禁用)
		if (context.isUseDefaultStyle()) {
			builder.registerWriteHandler(new LongestMatchColumnWidthStyleStrategy());
			builder.registerWriteHandler(ExcelStyleFactory.DEFAULT);
		}

		// 执行写入
		try (ExcelWriter writer = builder.build()) {
			for (Consumer<ExcelWriter> task : context.getSheetTasks()) {
				task.accept(writer);
			}
		}
	}

	/**
	 * 执行 CSV 写入.
	 * @param context CSV 写入上下文
	 * @param <T> 泛型类型
	 */
	public <T> void writeCsv(CsvWriteContext<T> context) {
		ExcelWriterBuilder builder = EasyExcel.write(context.outputStream(), context.clazz())
			.registerConverter(new ZonedDateTimeStringConverter());

		if (context.customizer() != null) {
			context.customizer().accept(builder);
		}

		var csvBuilder = builder.csv();

		Optional.ofNullable(context.delimiter())
			.map(CsvDelimiter::getValue)
			.map(String::valueOf)
			.ifPresent(csvBuilder::delimiter);
		Optional.ofNullable(context.quote()).map(CsvQuote::getValue).ifPresent(csvBuilder::quote);
		Optional.ofNullable(context.recordSeparator())
			.map(CsvRecordSeparator::getValue)
			.ifPresent(csvBuilder::recordSeparator);
		Optional.ofNullable(context.nullString()).ifPresent(csvBuilder::nullString);

		csvBuilder.doWrite(context.data());
	}

}
