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
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.service.SysDictTypeService;
import org.jax.snack.upms.api.vo.SysDictTypeVO;
import org.jax.snack.upms.biz.entity.SysDictType;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 字典类型 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/dict-types")
@RequiredArgsConstructor
public class SysDictTypeController {

	private final SysDictTypeService sysDictTypeService;

	/**
	 * 创建字典类型.
	 * @param dto 字典类型 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysDictTypeDTO dto) {
		this.sysDictTypeService.create(dto);
	}

	/**
	 * 按 ID 查询字典类型.
	 * @param id 字典类型 ID
	 * @return 分页结果（单条数据）
	 */
	@GetMapping("/{id}")
	public PageResult<SysDictTypeVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysDictType.Fields.id, id).build();
		return this.sysDictTypeService.queryByDsl(condition);
	}

	/**
	 * 按条件查询字典类型.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysDictTypeVO> query(@RequestBody QueryCondition condition) {
		return this.sysDictTypeService.queryByDsl(condition);
	}

	/**
	 * 更新字典类型.
	 * @param id 字典类型 ID.
	 * @param dto 字典类型 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDictTypeDTO dto) {
		this.sysDictTypeService.update(id, dto);
	}

	/**
	 * 删除字典类型.
	 * @param ids 字典类型 ID 列表.
	 */
	@DeleteMapping("/{ids}")
	public void delete(@PathVariable List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(SysDictType.Fields.id, ids).build();
		this.sysDictTypeService.deleteByDsl(condition);
	}

}
