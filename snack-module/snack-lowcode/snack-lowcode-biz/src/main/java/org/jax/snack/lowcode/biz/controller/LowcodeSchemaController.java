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

package org.jax.snack.lowcode.biz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.service.LowcodeSchemaService;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;

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
 * Schema 管理控制器.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode/schemas")
@RequiredArgsConstructor
public class LowcodeSchemaController {

	private final LowcodeSchemaService service;

	/**
	 * 创建 Schema.
	 * @param dto Schema DTO
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody LowcodeSchemaDTO dto) {
		this.service.create(dto);
	}

	/**
	 * 按 ID 查询 Schema.
	 * @param id Schema ID
	 * @return 分页结果
	 */
	@GetMapping("/{id}")
	public PageResult<LowcodeSchemaVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.id, id).build();
		return this.service.queryByDsl(condition);
	}

	/**
	 * 按条件查询 Schema.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<LowcodeSchemaVO> query(@RequestBody QueryCondition condition) {
		return this.service.queryByDsl(condition);
	}

	/**
	 * 更新 Schema.
	 * @param id Schema ID
	 * @param dto Schema DTO
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody LowcodeSchemaDTO dto) {
		this.service.update(id, dto);
	}

	/**
	 * 删除 Schema.
	 * @param ids Schema ID 列表
	 */
	@DeleteMapping("/{ids}")
	public void delete(@PathVariable List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(LowcodeSchema.Fields.id, ids).build();
		this.service.deleteByDsl(condition);
	}

	/**
	 * 发布 Schema, 对比变更并同步到物理表.
	 * @param id Schema ID
	 */
	@PostMapping("/{id}/publish")
	public void publish(@PathVariable Long id) {
		this.service.publishSchema(id);
	}

}
