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

package org.jax.snack.lowcode.api.service;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;

/**
 * Schema 管理服务接口.
 *
 * @author Jax Jiang
 */
public interface LowcodeSchemaService {

	/**
	 * 创建 Schema.
	 * @param dto Schema DTO
	 */
	void create(LowcodeSchemaDTO dto);

	/**
	 * 更新 Schema.
	 * @param id 主键 ID
	 * @param dto Schema DTO
	 */
	void update(Long id, LowcodeSchemaDTO dto);

	/**
	 * 根据条件删除 Schema.
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 使用 JSON DSL 查询 Schema.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<LowcodeSchemaVO> queryByDsl(QueryCondition condition);

	/**
	 * 发布 Schema, 对比变更并同步到物理表.
	 * @param id Schema ID
	 */
	void publishSchema(Long id);

}
