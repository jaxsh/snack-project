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
import java.util.Set;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.utils.tree.TreeNode;
import org.jax.snack.upms.api.dto.SysOrgDTO;
import org.jax.snack.upms.api.vo.SysOrgVO;

/**
 * 组织机构服务接口.
 *
 * @author Jax Jiang
 */
public interface SysOrgService {

	/**
	 * 创建组织机构.
	 * @param dto 组织机构 DTO.
	 */
	void create(SysOrgDTO dto);

	/**
	 * 更新组织机构.
	 * @param id 主键 ID.
	 * @param dto 组织机构 DTO.
	 */
	void update(Long id, SysOrgDTO dto);

	/**
	 * 删除组织机构及其所有子节点.
	 * @param id 主键 ID.
	 */
	void deleteById(Long id);

	/**
	 * 使用 JSON DSL 查询组织机构.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysOrgVO> queryByDsl(QueryCondition condition);

	/**
	 * 构建树形结构.
	 * @param orgCodes 可选, 指定从哪些节点开始构建子树, 不传则构建完整树
	 * @return 树形结构的根节点列表
	 */
	List<TreeNode<SysOrgVO>> buildTree(String... orgCodes);

	/**
	 * 查找指定节点的所有子孙节点 orgCode (包含自身).
	 * @param orgCodes 组织机构编码数组
	 * @return 所有子孙节点 orgCode 集合
	 */
	Set<String> findDescendantOrgCodes(String... orgCodes);

}
