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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.read.builder.ExcelReaderBuilder;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.context.AbstractReadContext;
import org.jax.snack.framework.excel.listener.ExcelDataListener;

/**
 * Excel 读取服务.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public class ExcelReadService {

	private final ExcelProperties excelProperties;

	private final Validator validator;

	/**
	 * 执行读取并处理数据 (基于 Context).
	 * @param context 读取上下文
	 * @param <T> 数据类型
	 */
	public <T> void read(AbstractReadContext<T> context) {
		this.read(context, null);
	}

	/**
	 * 执行读取并处理数据 (基于 Context，带保存回调).
	 * @param context 读取上下文
	 * @param saveFunction 保存回调
	 * @param <T> 数据类型
	 */
	public <T> void read(AbstractReadContext<T> context, Consumer<List<T>> saveFunction) {
		int batchSize = (context.batchSize() != null) ? context.batchSize()
				: this.excelProperties.getRead().getBatchSize();
		boolean failFast = (context.failFast() != null) ? context.failFast()
				: this.excelProperties.getRead().isFailFast();

		ExcelReaderBuilder builder;

		// 分情况构建不同泛型的 Listener
		if (context.converter() != null) {
			// Map 模式: R = Map<Integer, String>
			// EasyExcel 读取为 Map, Listener 通过 converter 转换为 T
			ExcelDataListener<T, Map<Integer, String>> listener = new ExcelDataListener<>(context.converter(),
					this.validator, saveFunction, context.businessValidator(), batchSize, failFast);
			builder = EasyExcel.read(context.inputStream(), listener);
		}
		else {
			// Bean 模式: R = T
			// EasyExcel 读取为 T, Listener 直接透传
			ExcelDataListener<T, T> listener = new ExcelDataListener<>((data, headers) -> data, this.validator,
					saveFunction, context.businessValidator(), batchSize, failFast);
			builder = EasyExcel.read(context.inputStream(), context.clazz(), listener);
		}

		if (context.customizer() != null) {
			context.customizer().accept(builder);
		}
		context.performRead(builder);
	}

}
