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
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;
import tools.jackson.databind.JsonNode;

/**
 * 元数据模板实体.
 * <p>
 * 存储实体/字段的定义模板 (JSON Schema 格式).
 * </p>
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString(callSuper = true)
@TableName(value = "lowcode_meta_schema", autoResultMap = true)
public class LowcodeMetaSchema extends BaseEntity {

	/**
	 * 模板类型 (entity, field).
	 */
	private String metaType;

	/**
	 * 版本.
	 */
	private String version;

	/**
	 * 模板 Schema (JSON Schema 格式).
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private JsonNode schemaJson;

	/**
	 * 描述.
	 */
	private String description;

}
