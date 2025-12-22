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

package org.jax.snack.lowcode.biz.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Schema 元信息.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Builder
public class SchemaMetadata {

	/**
	 * Java 实体名称 (e.g. SysTestProduct).
	 */
	private String entityName;

	/**
	 * 数据库表名 (e.g. sys_test_product).
	 */
	private String tableName;

	/**
	 * 显示标签.
	 */
	private String label;

}
