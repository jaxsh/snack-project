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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.api.vo.SysIdRuleVO;

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
 * ID 规则 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/id-rules")
@RequiredArgsConstructor
public class SysIdRuleController {

	private final SysIdRuleService sysIdRuleService;

	/**
	 * 创建 ID 规则.
	 * @param dto 规则 DTO
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysIdRuleDTO dto) {
		this.sysIdRuleService.create(dto);
	}

	/**
	 * 根据 ID 查询规则详情.
	 * @param id 规则 ID
	 * @return 规则详情 (含片段)
	 */
	@GetMapping("/{id}")
	public SysIdRuleVO getById(@PathVariable Long id) {
		return this.sysIdRuleService.getById(id);
	}

	/**
	 * 按条件查询规则列表.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysIdRuleVO> query(@RequestBody QueryCondition condition) {
		return this.sysIdRuleService.queryByDsl(condition);
	}

	/**
	 * 更新 ID 规则.
	 * @param id 规则 ID
	 * @param dto 规则 DTO
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysIdRuleDTO dto) {
		this.sysIdRuleService.update(id, dto);
	}

	/**
	 * 删除 ID 规则.
	 * @param id 规则 ID
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		this.sysIdRuleService.deleteById(id);
	}

}
