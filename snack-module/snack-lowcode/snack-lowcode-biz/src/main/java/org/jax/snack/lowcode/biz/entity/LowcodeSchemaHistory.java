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
 * Schema 版本历史实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
@TableName(value = "lowcode_schema_history", autoResultMap = true)
public class LowcodeSchemaHistory extends BaseEntity {

	/**
	 * Schema 名称.
	 */
	private String schemaName;

	/**
	 * 历史 Schema JSON.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private JsonNode schemaJson;

	/**
	 * 变更摘要.
	 */
	private String changeSummary;

}
