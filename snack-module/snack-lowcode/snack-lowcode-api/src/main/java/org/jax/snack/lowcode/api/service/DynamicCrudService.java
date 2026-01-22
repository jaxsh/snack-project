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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;

/**
 * 动态 CRUD 服务接口.
 *
 * @author Jax Jiang
 */
public interface DynamicCrudService {

	/**
	 * 创建数据.
	 * @param schemaName Schema 名称
	 * @param data 数据
	 */
	void create(String schemaName, Map<String, Object> data);

	/**
	 * 更新数据.
	 * @param schemaName Schema 名称
	 * @param id 主键
	 * @param data 数据
	 */
	void update(String schemaName, Long id, Map<String, Object> data);

	/**
	 * 删除数据 (批量).
	 * @param schemaName Schema 名称
	 * @param ids 主键列表
	 */
	void delete(String schemaName, List<Long> ids);

	/**
	 * 根据 ID 查询单条数据.
	 * @param schemaName Schema 名称
	 * @param id 主键
	 * @return 数据 (Optional)
	 */
	Optional<Map<String, Object>> getById(String schemaName, Long id);

	/**
	 * 分页查询.
	 * @param schemaName Schema 名称
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<Map<String, Object>> queryPage(String schemaName, QueryCondition condition);

}
