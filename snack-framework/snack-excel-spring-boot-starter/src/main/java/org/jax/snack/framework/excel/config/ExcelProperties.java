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

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Excel 配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snack.excel")
public class ExcelProperties {

	/**
	 * 读取配置.
	 */
	private Read read = new Read();

	/**
	 * 写入配置.
	 */
	private Write write = new Write();

	/**
	 * Excel 读取配置.
	 */
	@Getter
	@Setter
	public static class Read {

		/**
		 * 批次大小.
		 */
		private int batchSize = 500;

		/**
		 * 快速失败模式.
		 * <p>
		 * true: 遇到第一个错误立即抛出异常; false: 收集所有错误后统一抛出.
		 */
		private boolean failFast = false;

	}

	/**
	 * Excel 写入配置.
	 */
	@Getter
	@Setter
	public static class Write {

	}

}
