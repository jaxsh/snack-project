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

package org.jax.snack.upms.api.service;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysRoleDTO;
import org.jax.snack.upms.api.vo.SysRoleVO;

/**
 * 角色服务接口.
 *
 * @author Jax Jiang
 */
public interface SysRoleService {

	/**
	 * 创建角色.
	 * @param dto 角色 DTO
	 */
	void create(SysRoleDTO dto);

	/**
	 * 更新角色.
	 * @param id 主键 ID
	 * @param dto 角色 DTO
	 */
	void update(Long id, SysRoleDTO dto);

	/**
	 * 根据条件删除角色 (级联删除用户关联和资源关联).
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 使用 JSON DSL 查询角色.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysRoleVO> queryByDsl(QueryCondition condition);

}
