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

package org.jax.snack.framework.mybatisplus.context;

/**
 * 动态表名上下文持有者.
 * <p>
 * 使用 ThreadLocal 存储当前线程需要操作的物理表名. 可用于分表场景或动态表单场景.
 * </p>
 *
 * @author Jax Jiang
 */
public final class TableContextHolder {

	private TableContextHolder() {
	}

	private static final ThreadLocal<String> TABLE_NAME = new ThreadLocal<>();

	/**
	 * 设置当前线程的表名.
	 * @param tableName 物理表名
	 */
	public static void setTableName(String tableName) {
		TABLE_NAME.set(tableName);
	}

	/**
	 * 获取当前线程的表名.
	 * @return 物理表名
	 */
	public static String getTableName() {
		return TABLE_NAME.get();
	}

	/**
	 * 清除当前线程的表名.
	 */
	public static void clear() {
		TABLE_NAME.remove();
	}

}
