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
import java.util.function.Consumer;

import cn.idev.excel.read.builder.ExcelReaderBuilder;
import cn.idev.excel.read.builder.ExcelReaderSheetBuilder;
import lombok.experimental.Accessors;
import org.jax.snack.framework.excel.ExcelReadService;

import org.springframework.util.StringUtils;

/**
 * Excel 读取上下文.
 * <p>
 * 提供链式调用接口用于配置 Excel 读取参数.
 *
 * @param <T> 数据类型
 * @author Jax Jiang
 */
@Accessors(chain = true, fluent = true)
public class ExcelReadContext<T> extends AbstractReadContext<T> {

	/**
	 * 构造函数.
	 * @param service Excel 读取服务
	 * @param inputStream 输入流
	 * @param clazz 数据类型
	 */
	public ExcelReadContext(ExcelReadService service, InputStream inputStream, Class<T> clazz) {
		super(service, inputStream, clazz);
	}

	/**
	 * 执行读取并处理数据.
	 * @param saveFunction 业务处理函数
	 */
	@Override
	public void doRead(Consumer<List<T>> saveFunction) {
		super.doRead(saveFunction, (actualBatchSize, actualFailFast) -> this.service.read(this, saveFunction));
	}

	@Override
	public void performRead(ExcelReaderBuilder builder) {
		ExcelReaderSheetBuilder sheetBuilder;
		if (this.readConfig != null) {
			if (StringUtils.hasText(this.readConfig.getSheetName())) {
				sheetBuilder = builder.sheet(this.readConfig.getSheetName());
			}
			else if (this.readConfig.getSheetNo() != null) {
				sheetBuilder = builder.sheet(this.readConfig.getSheetNo());
			}
			else {
				sheetBuilder = builder.sheet();
			}
			if (this.readConfig.getHeadRowNumber() != null) {
				sheetBuilder.headRowNumber(this.readConfig.getHeadRowNumber());
			}
		}
		else {
			sheetBuilder = builder.sheet();
		}
		sheetBuilder.doRead();
	}

}
