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

package org.jax.snack.lowcode.biz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 数据类型实体.
 * <p>
 * 存储 Liquibase 标准类型定义.
 * </p>
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString(callSuper = true)
@TableName("lowcode_data_type")
public class LowcodeDataType extends BaseEntity {

	/**
	 * 逻辑类型(前端) (string, int, decimal...).
	 */
	private String logicType;

	/**
	 * 数据库类型 (varchar, int, decimal...).
	 */
	private String dbType;

	/**
	 * JSON Schema 类型 (string, integer, number, boolean).
	 */
	private String jsonSchemaType;

	/**
	 * 显示名称.
	 */
	private String label;

	/**
	 * 是否需要长度参数.
	 */
	private Boolean needLength;

	/**
	 * 是否需要精度参数.
	 */
	private Boolean needScale;

	/**
	 * 默认长度.
	 */
	private Integer defaultLength;

	/**
	 * 默认精度.
	 */
	private Integer defaultScale;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 是否启用.
	 */
	private Boolean enabled;

}
