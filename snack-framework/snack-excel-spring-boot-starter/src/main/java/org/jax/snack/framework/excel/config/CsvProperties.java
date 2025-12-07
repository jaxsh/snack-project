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

import cn.idev.excel.metadata.csv.CsvConstant;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.jax.snack.framework.excel.enums.CsvQuote;
import org.jax.snack.framework.excel.enums.CsvRecordSeparator;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CSV 配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snack.csv")
public class CsvProperties {

	/**
	 * CSV 读取配置.
	 */
	private CsvConfig read = new CsvConfig();

	/**
	 * CSV 写入配置.
	 */
	private CsvConfig write = new CsvConfig();

	/**
	 * CSV 具体配置.
	 */
	@Getter
	@Setter
	public static class CsvConfig {

		/**
		 * 字段分隔符.
		 */
		private CsvDelimiter delimiter = CsvDelimiter.COMMA;

		/**
		 * 引号字符.
		 */
		private CsvQuote quote = CsvQuote.DOUBLE_QUOTE;

		/**
		 * 行分隔符.
		 */
		private CsvRecordSeparator recordSeparator = CsvRecordSeparator.CRLF;

		/**
		 * null 值显示.
		 */
		private String nullString = CsvConstant.EMPTY;

	}

}
