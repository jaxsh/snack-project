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

package org.jax.snack.lowcode.biz.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jax.snack.lowcode.api.enums.SchemaStatus;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;

/**
 * Schema Mapper.
 *
 * @author Jax Jiang
 */
@Mapper
public interface LowcodeSchemaMapper extends BaseMapper<LowcodeSchema> {

	/**
	 * 根据 Schema 名称和状态查询.
	 * @param schemaName Schema 名称
	 * @param status 状态
	 * @return Schema 实体
	 */
	default LowcodeSchema selectByNameAndStatus(String schemaName, SchemaStatus status) {
		return selectOne(new LambdaQueryWrapper<LowcodeSchema>().eq(LowcodeSchema::getSchemaName, schemaName)
			.eq(LowcodeSchema::getStatus, status.getCode()));
	}

	/**
	 * 查询指定的草稿.
	 * @param schemaName Schema 名称
	 * @return Schema 实体
	 */
	default LowcodeSchema selectDraftByName(String schemaName) {
		return selectByNameAndStatus(schemaName, SchemaStatus.DRAFT);
	}

	/**
	 * 查询指定的已发布版本.
	 * @param schemaName Schema 名称
	 * @return Schema 实体
	 */
	default LowcodeSchema selectPublishedByName(String schemaName) {
		return selectByNameAndStatus(schemaName, SchemaStatus.PUBLISHED);
	}

	/**
	 * 根据 API 路径查询已发布的 Schema.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return Schema 实体
	 */
	default LowcodeSchema selectPublishedByResourcePath(String resourcePath) {
		return selectOne(new LambdaQueryWrapper<LowcodeSchema>().eq(LowcodeSchema::getResourcePath, resourcePath)
			.eq(LowcodeSchema::getStatus, SchemaStatus.PUBLISHED.getCode()));
	}

}
