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

package org.jax.snack.oauth.biz.controller;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.oauth.api.dto.RegisteredClientDTO;
import org.jax.snack.oauth.api.service.OAuth2RegisteredClientService;
import org.jax.snack.oauth.api.vo.RegisteredClientVO;
import org.jax.snack.oauth.biz.entity.OAuth2RegisteredClient;

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
 * OAuth2 客户端 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/oauth2/clients")
@RequiredArgsConstructor
public class OAuth2RegisteredClientController {

	private final OAuth2RegisteredClientService service;

	/**
	 * 创建客户端.
	 * @param dto 客户端 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody RegisteredClientDTO dto) {
		this.service.create(dto);
	}

	/**
	 * 按 ID 查询客户端.
	 * @param id 客户端 ID
	 * @return 分页结果（单条数据）
	 */
	@GetMapping("/{id}")
	public PageResult<RegisteredClientVO> getById(@PathVariable String id) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2RegisteredClient.Fields.id, id).build();
		return this.service.queryByDsl(condition);
	}

	/**
	 * 按条件查询客户端.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<RegisteredClientVO> query(@RequestBody QueryCondition condition) {
		return this.service.queryByDsl(condition);
	}

	/**
	 * 更新客户端.
	 * @param id 客户端 ID.
	 * @param dto 客户端 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable String id, @Validated(Update.class) @RequestBody RegisteredClientDTO dto) {
		this.service.update(id, dto);
	}

	/**
	 * 删除客户端.
	 * @param id 客户端 ID.
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		this.service.deleteById(id);
	}

}
