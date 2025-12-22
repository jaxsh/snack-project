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

package org.jax.snack.lowcode.api.vo;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Schema VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class LowcodeSchemaVO {

	/**
	 * 主键ID.
	 */
	private Long id;

	/**
	 * Schema 名称 (唯一标识).
	 */
	private String schemaName;

	/**
	 * RESTful API 路径 (kebab-case, e.g. sys-employee).
	 */
	private String resourcePath;

	/**
	 * JSON Schema 定义 (字符串形式).
	 */
	private Object schemaJson;

	/**
	 * 显示名称.
	 */
	private String label;

	/**
	 * 描述.
	 */
	private String description;

	/**
	 * 分类.
	 */
	private String category;

	/**
	 * 状态.
	 */
	private Integer status;

	/**
	 * 状态名称.
	 */
	private String statusLabel;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
