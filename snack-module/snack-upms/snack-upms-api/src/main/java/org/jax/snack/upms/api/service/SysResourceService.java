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

import java.util.List;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.utils.tree.TreeNode;
import org.jax.snack.upms.api.dto.SysResourceDTO;
import org.jax.snack.upms.api.vo.SysResourceVO;

/**
 * 资源服务接口.
 *
 * @author Jax Jiang
 */
public interface SysResourceService {

	/**
	 * 创建资源.
	 * @param dto 资源 DTO
	 */
	void create(SysResourceDTO dto);

	/**
	 * 更新资源.
	 * @param id 主键 ID
	 * @param dto 资源 DTO
	 */
	void update(Long id, SysResourceDTO dto);

	/**
	 * 根据条件删除资源 (级联删除子节点).
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 使用 JSON DSL 查询资源.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysResourceVO> queryByDsl(QueryCondition condition);

	/**
	 * 构建资源树.
	 * @return 树形结构
	 */
	List<TreeNode<SysResourceVO>> buildTree();

	/**
	 * 查询角色拥有的资源集合.
	 * @param roleCode 角色编码
	 * @return 资源 VO 列表
	 */
	List<SysResourceVO> findResourcesByRoleCode(String roleCode);

}
