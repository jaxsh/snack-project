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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import cn.idev.excel.read.builder.ExcelReaderBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jax.snack.framework.excel.ExcelReadService;
import org.jax.snack.framework.excel.config.ExcelReadConfig;

/**
 * 抽象的读取上下文.
 * <p>
 * 提供链式调用接口用于配置读取参数.
 *
 * @param <T> 数据类型
 * @author Jax Jiang
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@RequiredArgsConstructor
public abstract class AbstractReadContext<T> {

	protected final ExcelReadService service;

	protected final InputStream inputStream;

	protected final Class<T> clazz;

	protected Integer batchSize;

	protected Boolean failFast;

	protected Consumer<T> businessValidator;

	protected Consumer<ExcelReaderBuilder> customizer;

	protected ExcelReadConfig readConfig;

	/**
	 * 数据转换器 (可选).
	 * <p>
	 * 接收两个参数: rowData (列索引 to 单元格值), columnToHeader (列索引 to 表头名称).
	 */
	protected BiFunction<Map<Integer, String>, Map<Integer, String>, T> converter;

	/**
	 * 配置自定义构建器回调.
	 * @param customizer 回调函数
	 * @return this
	 */
	public AbstractReadContext<T> customize(Consumer<ExcelReaderBuilder> customizer) {
		this.customizer = customizer;
		return this;
	}

	/**
	 * 应用读取配置.
	 * @param config 读取配置
	 * @return this
	 */
	public AbstractReadContext<T> applyConfig(ExcelReadConfig config) {
		this.readConfig = config;
		return this;
	}

	/**
	 * 执行读取并处理数据.
	 * @param saveFunction 业务处理函数
	 * @param readOperation BiConsumer，接受 batchSize 和 failFast，并执行具体的读取操作
	 */
	protected void doRead(Consumer<List<T>> saveFunction, BiConsumer<Integer, Boolean> readOperation) {
		int actualBatchSize = (this.batchSize != null) ? this.batchSize
				: this.service.getExcelProperties().getRead().getBatchSize();
		boolean actualFailFast = (this.failFast != null) ? this.failFast
				: this.service.getExcelProperties().getRead().isFailFast();

		readOperation.accept(actualBatchSize, actualFailFast);
	}

	/**
	 * 执行具体的读取操作.
	 * @param builder EasyExcel Reader Builder
	 */
	public abstract void performRead(ExcelReaderBuilder builder);

	/**
	 * 抽象方法，子类实现具体的读取逻辑.
	 * @param saveFunction 业务处理函数
	 */
	public abstract void doRead(Consumer<List<T>> saveFunction);

}
