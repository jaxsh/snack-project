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

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

import cn.idev.excel.write.builder.ExcelWriterBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jax.snack.framework.excel.ExcelWriteService;
import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.jax.snack.framework.excel.enums.CsvQuote;
import org.jax.snack.framework.excel.enums.CsvRecordSeparator;

/**
 * CSV 写入上下文.
 * <p>
 * 提供链式调用接口用于配置 CSV 写入参数.
 *
 * @param <T> 数据类型
 * @author Jax Jiang
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@RequiredArgsConstructor
public class CsvWriteContext<T> {

	private final ExcelWriteService service;

	private final OutputStream outputStream;

	private final List<T> data;

	private final Class<T> clazz;

	private CsvDelimiter delimiter;

	private CsvQuote quote;

	private CsvRecordSeparator recordSeparator;

	private String nullString;

	private Consumer<ExcelWriterBuilder> customizer;

	/**
	 * 配置自定义构建器回调.
	 * @param customizer 回调函数
	 * @return this
	 */
	public CsvWriteContext<T> customize(Consumer<ExcelWriterBuilder> customizer) {
		this.customizer = customizer;
		return this;
	}

	/**
	 * 执行写入.
	 */
	public void doWrite() {
		this.service.writeCsv(this);
	}

}
