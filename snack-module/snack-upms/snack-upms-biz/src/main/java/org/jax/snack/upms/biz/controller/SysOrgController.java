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

package org.jax.snack.upms.biz.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.framework.utils.tree.TreeNode;
import org.jax.snack.upms.api.dto.SysOrgDTO;
import org.jax.snack.upms.api.service.SysOrgService;
import org.jax.snack.upms.api.vo.SysOrgVO;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织机构 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/orgs")
@RequiredArgsConstructor
public class SysOrgController {

	private final SysOrgService sysOrgService;

	/**
	 * 创建组织机构.
	 * @param dto 组织机构 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysOrgDTO dto) {
		this.sysOrgService.create(dto);
	}

	/**
	 * 按 ID 查询组织机构.
	 * @param id 组织机构 ID
	 * @return 分页结果 (单条数据)
	 */
	@GetMapping("/{id}")
	public PageResult<SysOrgVO> getById(@PathVariable Long id) {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("id", Map.of(QueryOperator.EQ.getValue(), id)));
		return this.sysOrgService.queryByDsl(condition);
	}

	/**
	 * 按条件查询组织机构.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysOrgVO> query(@RequestBody QueryCondition condition) {
		return this.sysOrgService.queryByDsl(condition);
	}

	/**
	 * 获取树形结构.
	 * @param orgCodes 可选, 指定从哪些节点开始构建子树
	 * @return 树形结构
	 */
	@GetMapping("/tree")
	public List<TreeNode<SysOrgVO>> tree(@RequestParam(required = false) String[] orgCodes) {
		if (orgCodes == null || orgCodes.length == 0) {
			return this.sysOrgService.buildTree();
		}
		return this.sysOrgService.buildTree(orgCodes);
	}

	/**
	 * 查找指定节点的所有子孙节点 orgCode.
	 * @param orgCodes 组织机构编码数组
	 * @return 所有子孙节点 orgCode 集合
	 */
	@GetMapping("/descendants")
	public Set<String> findDescendants(@RequestParam String[] orgCodes) {
		return this.sysOrgService.findDescendantOrgCodes(orgCodes);
	}

	/**
	 * 更新组织机构.
	 * @param id 组织机构 ID.
	 * @param dto 组织机构 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysOrgDTO dto) {
		this.sysOrgService.update(id, dto);
	}

	/**
	 * 删除组织机构及其所有子节点.
	 * @param id 组织机构 ID.
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		this.sysOrgService.deleteById(id);
	}

}
