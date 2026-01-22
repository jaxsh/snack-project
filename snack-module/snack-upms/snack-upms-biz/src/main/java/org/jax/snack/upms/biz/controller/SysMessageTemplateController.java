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
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysMessageTemplateDTO;
import org.jax.snack.upms.api.service.SysMessageTemplateService;
import org.jax.snack.upms.api.vo.SysMessageTemplateVO;
import org.jax.snack.upms.biz.entity.SysMessageTemplate;

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
 * 消息模版控制器.
 *
 * @author Jax Jiang
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/message-template")
public class SysMessageTemplateController {

	private final SysMessageTemplateService sysMessageTemplateService;

	/**
	 * 分页查询消息模版 (DSL).
	 * @param condition 查询条件
	 * @return 分页列表
	 */
	@PostMapping("/query")
	public PageResult<SysMessageTemplateVO> query(@RequestBody QueryCondition condition) {
		return this.sysMessageTemplateService.queryByDsl(condition);
	}

	/**
	 * 获取消息模版详情.
	 * @param id ID
	 * @return 详情
	 */
	@GetMapping("/{id}")
	public PageResult<SysMessageTemplateVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysMessageTemplate.Fields.id, id).build();
		return this.sysMessageTemplateService.queryByDsl(condition);
	}

	/**
	 * 新增消息模版.
	 * @param dto 数据
	 */
	@PostMapping
	public void save(@Validated(Create.class) @RequestBody SysMessageTemplateDTO dto) {
		this.sysMessageTemplateService.create(dto);
	}

	/**
	 * 修改消息模版.
	 * @param id ID
	 * @param dto 数据
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysMessageTemplateDTO dto) {
		this.sysMessageTemplateService.update(id, dto);
	}

	/**
	 * 删除消息模版.
	 * @param ids ID 列表
	 */
	@DeleteMapping("/{ids}")
	public void removeById(@PathVariable java.util.List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(SysMessageTemplate.Fields.id, ids).build();
		this.sysMessageTemplateService.deleteByDsl(condition);
	}

}
