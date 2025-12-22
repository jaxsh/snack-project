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
import org.jax.snack.lowcode.api.vo.LowcodeDataTypeVO;
import org.jax.snack.lowcode.biz.service.schema.LowcodeDataTypeService;
import tools.jackson.databind.JsonNode;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据类型控制器.
 * <p>
 * 提供元数据模板 API.
 * </p>
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode/meta")
@RequiredArgsConstructor
public class LowcodeDataTypeController {

	private final LowcodeDataTypeService lowcodeDataTypeService;

	/**
	 * 获取所有数据类型.
	 * @return 数据类型列表
	 */
	@GetMapping("/data-types")
	public List<LowcodeDataTypeVO> getDataTypes() {
		return this.lowcodeDataTypeService.getDataTypes();
	}

	/**
	 * 获取实体模板 Schema.
	 * @return JSON Schema
	 */
	@GetMapping("/entity-template")
	public JsonNode getEntityTemplate() {
		return this.lowcodeDataTypeService.getEntityTemplate();
	}

	/**
	 * 获取字段模板 Schema.
	 * @return JSON Schema
	 */
	@GetMapping("/field-template")
	public JsonNode getFieldTemplate() {
		return this.lowcodeDataTypeService.getFieldTemplate();
	}

}
