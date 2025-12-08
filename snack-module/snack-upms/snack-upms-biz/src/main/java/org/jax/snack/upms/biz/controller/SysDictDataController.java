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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysDictDataDTO;
import org.jax.snack.upms.api.service.SysDictDataService;
import org.jax.snack.upms.api.vo.SysDictDataVO;

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
 * 字典数据 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/dict-data")
@RequiredArgsConstructor
public class SysDictDataController {

	private final SysDictDataService service;

	/**
	 * 创建字典数据.
	 * @param dto 字典数据 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysDictDataDTO dto) {
		this.service.create(dto);
	}

	/**
	 * 按 ID 查询字典数据.
	 * @param id 字典数据 ID
	 * @return 分页结果（单条数据）
	 */
	@GetMapping("/{id}")
	public PageResult<SysDictDataVO> getById(@PathVariable Long id) {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("id", Map.of(QueryOperator.EQ.getValue(), id)));
		return this.service.queryByDsl(condition);
	}

	/**
	 * 按条件查询字典数据.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysDictDataVO> query(@RequestBody QueryCondition condition) {
		return this.service.queryByDsl(condition);
	}

	/**
	 * 根据字典类型查询字典数据列表.
	 * @param dictType 字典类型
	 * @return 字典数据列表
	 */
	@GetMapping("/type/{dictType}")
	public List<SysDictDataVO> getByDictType(@PathVariable String dictType) {
		return this.service.getByDictType(dictType);
	}

	/**
	 * 更新字典数据.
	 * @param id 字典数据 ID.
	 * @param dto 字典数据 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysDictDataDTO dto) {
		this.service.update(id, dto);
	}

	/**
	 * 删除字典数据.
	 * @param id 字典数据 ID.
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		this.service.deleteById(id);
	}

}
