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
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.lowcode.biz.options.OptionItem;
import org.jax.snack.lowcode.biz.service.crud.DynamicCrudService;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动态 CRUD 控制器.
 * <p>
 * 提供基于 Schema 的通用 CRUD 接口. URL 路径使用 kebab-case (e.g. /api/lowcode/sys-employee).
 * </p>
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode")
@RequiredArgsConstructor
public class DynamicCrudController {

	private final SchemaService schemaService;

	private final DynamicCrudService dynamicCrudService;

	/**
	 * 获取 Schema 定义.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return JSON Schema
	 */
	@GetMapping("/{resourcePath}/schema")
	public JsonNode getSchema(@PathVariable String resourcePath) {
		return this.schemaService.getSchemaByResourcePath(resourcePath);
	}

	/**
	 * 创建数据.
	 * @param resourcePath API 路径 (kebab-case)
	 * @param data 数据
	 */
	@PostMapping("/{resourcePath}")
	public void create(@PathVariable String resourcePath, @RequestBody Map<String, Object> data) {
		String schemaName = this.schemaService.getSchemaNameByResourcePath(resourcePath);
		this.dynamicCrudService.create(schemaName, data);
	}

	/**
	 * 更新数据.
	 * @param resourcePath API 路径 (kebab-case)
	 * @param id 主键
	 * @param data 数据
	 */
	@PutMapping("/{resourcePath}/{id}")
	public void update(@PathVariable String resourcePath, @PathVariable Long id,
			@RequestBody Map<String, Object> data) {
		String schemaName = this.schemaService.getSchemaNameByResourcePath(resourcePath);
		this.dynamicCrudService.update(schemaName, id, data);
	}

	/**
	 * 删除数据.
	 * @param resourcePath API 路径 (kebab-case)
	 * @param id 主键
	 */
	@DeleteMapping("/{resourcePath}/{id}")
	public void delete(@PathVariable String resourcePath, @PathVariable Long id) {
		String schemaName = this.schemaService.getSchemaNameByResourcePath(resourcePath);
		this.dynamicCrudService.delete(schemaName, id);
	}

	/**
	 * 分页查询.
	 * @param resourcePath API 路径 (kebab-case)
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/{resourcePath}/query")
	public PageResult<Map<String, Object>> queryPage(@PathVariable String resourcePath,
			@RequestBody QueryCondition condition) {
		String schemaName = this.schemaService.getSchemaNameByResourcePath(resourcePath);
		return this.dynamicCrudService.queryPage(schemaName, condition);
	}

	/**
	 * 获取字段选项列表.
	 * @param resourcePath API 路径 (kebab-case)
	 * @param fieldName 字段名
	 * @return 选项列表
	 */
	@GetMapping("/{resourcePath}/options/{fieldName}")
	public List<OptionItem> getOptions(@PathVariable String resourcePath, @PathVariable String fieldName) {
		String schemaName = this.schemaService.getSchemaNameByResourcePath(resourcePath);
		return this.schemaService.getFieldOptions(schemaName, fieldName);
	}

}
