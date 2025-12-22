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
import tools.jackson.databind.JsonNode;

/**
 * 字段定义.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Builder
public class FieldDefinition {

	/**
	 * Java 字段名 (e.g. productCode).
	 */
	private String fieldName;

	/**
	 * 数据库列名 (e.g. product_code).
	 */
	private String dbColumn;

	/**
	 * JSON Schema 类型 (e.g. string, integer, number).
	 */
	private String type;

	/**
	 * 显示标签.
	 */
	private String label;

	/**
	 * 是否必填.
	 */
	private boolean required;

	/**
	 * 是否在列表中可见.
	 */
	private boolean listVisible;

	/**
	 * 列宽.
	 */
	private Integer width;

	/**
	 * 控件类型 (e.g. Input, Select).
	 */
	private String widget;

	/**
	 * x-options 配置节点 (用于选项翻译).
	 */
	private JsonNode xOptions;

}
