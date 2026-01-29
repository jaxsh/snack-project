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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;
import tools.jackson.databind.JsonNode;

/**
 * 模型定义实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
@FieldNameConstants
@TableName(value = "lowcode_schema", autoResultMap = true)
public class LowcodeSchema extends BaseEntity {

	/**
	 * 模型名称.
	 */
	private String schemaName;

	/**
	 * 接口路径.
	 */
	private String resourcePath;

	/**
	 * JSON模型定义.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private JsonNode schemaJson;

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
	 * 状态(0:草稿, 1:已发布).
	 */
	private Integer status;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
