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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysRoleDTO;
import org.jax.snack.upms.api.service.SysResourceService;
import org.jax.snack.upms.api.service.SysRoleService;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysRoleVO;
import org.jax.snack.upms.biz.entity.SysRole;

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
 * 角色管理 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/roles")
@RequiredArgsConstructor
public class SysRoleController {

	private final SysRoleService service;

	private final SysResourceService sysResourceService;

	/**
	 * 创建角色.
	 * @param dto 角色 DTO
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysRoleDTO dto) {
		this.service.create(dto);
	}

	/**
	 * 按 ID 查询角色.
	 * @param id 角色 ID
	 * @return 分页结果 (单条数据)
	 */
	@GetMapping("/{id}")
	public PageResult<SysRoleVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysRole.Fields.id, id).build();
		return this.service.queryByDsl(condition);
	}

	/**
	 * 更新角色.
	 * @param id 角色 ID
	 * @param dto 角色 DTO
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysRoleDTO dto) {
		this.service.update(id, dto);
	}

	/**
	 * 删除角色.
	 * @param ids 角色 ID 列表.
	 */
	@DeleteMapping("/{ids}")
	public void delete(@PathVariable List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(SysRole.Fields.id, ids).build();
		this.service.deleteByDsl(condition);
	}

	/**
	 * 分页查询角色.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysRoleVO> query(@RequestBody QueryCondition condition) {
		return this.service.queryByDsl(condition);
	}

	/**
	 * 获取角色拥有的资源集合.
	 * @param roleCode 角色编码
	 * @param status 资源状态（可选，null 时返回全部）
	 * @return 资源 VO 列表
	 */
	@GetMapping("/{roleCode}/resources")
	public List<SysResourceVO> getRoleResources(@PathVariable String roleCode,
			@RequestParam(required = false) Integer status) {
		return this.sysResourceService.findResourcesByRoleCode(roleCode, status);
	}

}
