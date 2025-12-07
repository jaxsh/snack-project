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

package org.jax.snack.framework.excel.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 文件名生成工具.
 *
 * @author Jax Jiang
 */
public final class FileNameGenerator {

	private FileNameGenerator() {
	}

	/**
	 * 生成带时间戳的文件名.
	 * <p>
	 * 格式: ClassName_yyyyMMdd_HHmmss
	 * @param clazz 数据类型
	 * @return 文件名 (不含扩展名)
	 */
	public static String generate(Class<?> clazz) {
		String className = clazz.getSimpleName();
		LocalDateTime now = LocalDateTime.now();

		String date = now.format(DateTimeFormatter.BASIC_ISO_DATE);
		String time = now.format(DateTimeFormatter.ofPattern("HHmmss"));

		return className + "_" + date + "_" + time;
	}

}
