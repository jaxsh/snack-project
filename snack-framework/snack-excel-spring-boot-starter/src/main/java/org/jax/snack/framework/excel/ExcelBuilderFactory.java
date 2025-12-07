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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import cn.idev.excel.write.builder.ExcelWriterBuilder;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.excel.context.CsvReadContext;
import org.jax.snack.framework.excel.context.CsvWriteContext;
import org.jax.snack.framework.excel.context.ExcelReadContext;
import org.jax.snack.framework.excel.context.ExcelWriteContext;

/**
 * Excel 构建器工厂.
 * <p>
 * 提供统一的入口用于构建 Excel 导入/导出任务.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class ExcelBuilderFactory {

	private final ExcelReadService readService;

	private final ExcelWriteService writeService;

	/**
	 * 执行 Excel 读取 (DSL 风格).
	 * @param inputStream 输入流
	 * @param clazz 数据类型
	 * @param consumer 上下文配置回调
	 * @param <T> 数据类型
	 */
	public <T> void read(InputStream inputStream, Class<T> clazz, Consumer<ExcelReadContext<T>> consumer) {
		consumer.accept(new ExcelReadContext<>(this.readService, inputStream, clazz));
	}

	/**
	 * 执行 CSV 读取 (DSL 风格).
	 * @param inputStream 输入流
	 * @param clazz 数据类型
	 * @param consumer 上下文配置回调
	 * @param <T> 数据类型
	 */
	public <T> void readCsv(InputStream inputStream, Class<T> clazz, Consumer<CsvReadContext<T>> consumer) {
		consumer.accept(new CsvReadContext<>(this.readService, inputStream, clazz));
	}

	/**
	 * 执行 CSV 写入 (DSL 风格).
	 * @param outputStream 输出流
	 * @param data 数据列表
	 * @param clazz 数据类型
	 * @param consumer 上下文配置回调
	 * @param <T> 数据类型
	 */
	public <T> void writeCsv(OutputStream outputStream, List<T> data, Class<T> clazz,
			Consumer<CsvWriteContext<T>> consumer) {
		consumer.accept(new CsvWriteContext<>(this.writeService, outputStream, data, clazz));
	}

	/**
	 * 执行基于上下文的 Excel 写入 (统一入口).
	 * <p>
	 * 支持单 Sheet (自动应用默认样式) 和多 Sheet 复杂导出. 默认自动应用 {@code ExcelStyleFactory.DEFAULT} 样式和
	 * {@code LongestMatchColumnWidthStyleStrategy} 自动列宽. 如需禁用默认样式，请调用
	 * {@code ctx.ignoreDefaultStyle()}.
	 * <p>
	 * 示例: {@code factory.write(os, ctx -> ctx.sheet("Data", list, VO.class));}
	 * @param outputStream 输出流
	 * @param contextConsumer 上下文配置回调
	 */
	public void write(OutputStream outputStream, Consumer<ExcelWriteContext> contextConsumer) {
		ExcelWriterBuilder builder = this.writeService.createWriterBuilder(outputStream);
		ExcelWriteContext context = new ExcelWriteContext(builder);
		contextConsumer.accept(context);
		this.writeService.write(context);
	}

}
