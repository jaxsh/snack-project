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

package org.jax.snack.framework.excel.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.exception.ExcelDataConvertException;
import cn.idev.excel.metadata.data.ReadCellData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

import org.springframework.util.CollectionUtils;

/**
 * Excel 数据读取监听器.
 * <p>
 * 提供统一的读取功能: 校验、批量处理、快速失败、错误收集.
 *
 * @param <T> 目标数据类型 (入库类型)
 * @param <R> 读取数据类型 (Excel 原始类型, 例如 T 或 Map)
 * @author Jax Jiang
 */
@Slf4j
public class ExcelDataListener<T, R> extends AnalysisEventListener<R> {

	private final Validator validator;

	private final Consumer<List<T>> saveFunction;

	private final Consumer<T> businessValidator;

	private final int batchSize;

	private final boolean failFast;

	/**
	 * 数据处理器/转换器.
	 * <p>
	 * 将读取到的数据 R 转换为目标数据 T. 接收参数: (readData, columnToHeaderMap) -> targetData
	 */
	private final BiFunction<R, Map<Integer, String>, T> processor;

	/**
	 * 运行时构建: 列索引 -> 表头名称.
	 */
	private final Map<Integer, String> columnToHeader = new HashMap<>();

	private final List<T> cachedData = new ArrayList<>();

	private final Set<ConstraintViolation<?>> allViolations = new HashSet<>();

	private final List<Integer> affectedRows = new ArrayList<>();

	/**
	 * 构造器.
	 * @param processor 数据处理器 (readData, columnToHeader) -> T
	 * @param validator Jakarta Validation 校验器
	 * @param saveFunction 业务处理函数
	 * @param businessValidator 业务校验回调 (可选)
	 * @param batchSize 批次大小
	 * @param failFast 是否快速失败
	 */
	public ExcelDataListener(BiFunction<R, Map<Integer, String>, T> processor, Validator validator,
			Consumer<List<T>> saveFunction, Consumer<T> businessValidator, int batchSize, boolean failFast) {
		this.processor = processor;
		this.validator = validator;
		this.saveFunction = saveFunction;
		this.businessValidator = businessValidator;
		this.batchSize = batchSize;
		this.failFast = failFast;
	}

	@Override
	public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
		// 收集表头信息: 列索引 -> 表头名称
		headMap.forEach((columnIndex, cellData) -> this.columnToHeader.put(columnIndex, cellData.getStringValue()));
	}

	@Override
	public void invoke(R data, AnalysisContext context) {
		int rowIndex = context.readRowHolder().getRowIndex();

		// 转换数据 (类型安全，无需 cast)
		T result = this.processor.apply(data, this.columnToHeader);

		// 校验
		if (this.validator != null) {
			Set<ConstraintViolation<T>> violations = this.validator.validate(result);
			if (!violations.isEmpty()) {
				this.allViolations.addAll(violations);
				this.affectedRows.add(rowIndex);

				if (this.failFast) {
					throw new ConstraintViolationException(String.format("Validation failed at row %d", rowIndex),
							violations);
				}
				return;
			}
		}

		// 业务校验
		if (this.businessValidator != null) {
			this.businessValidator.accept(result);
		}

		this.cachedData.add(result);

		if (this.cachedData.size() >= this.batchSize) {
			processBatch();
		}
	}

	@Override
	public void onException(Exception exception, AnalysisContext context) {
		if (exception instanceof ExcelDataConvertException convertException) {
			log.error("Row {}, Column {} data conversion error, data: {}", convertException.getRowIndex(),
					convertException.getColumnIndex(), convertException.getCellData());
			throw convertException;
		}

		if (exception instanceof ConstraintViolationException validationException) {
			if (this.failFast) {
				throw validationException;
			}
			this.allViolations.addAll(validationException.getConstraintViolations());
			return;
		}

		int rowIndex = context.readRowHolder().getRowIndex();
		log.error("Row {} import error: {}", rowIndex, exception.getMessage(), exception);
		throw new RuntimeException("Excel import error at row " + rowIndex, exception);
	}

	@Override
	public void doAfterAllAnalysed(AnalysisContext context) {
		if (!this.cachedData.isEmpty()) {
			processBatch();
		}

		if (!this.allViolations.isEmpty()) {
			throw new ConstraintViolationException(String.format("Validation failed at rows %s", this.affectedRows),
					this.allViolations);
		}
	}

	private void processBatch() {
		if (!CollectionUtils.isEmpty(this.cachedData)) {
			this.saveFunction.accept(new ArrayList<>(this.cachedData));
			this.cachedData.clear();
		}
	}

}
