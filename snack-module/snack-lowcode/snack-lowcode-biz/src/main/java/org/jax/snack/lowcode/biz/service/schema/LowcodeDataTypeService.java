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

package org.jax.snack.lowcode.biz.service.schema;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.OrderByCondition;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.query.SortDirection;
import org.jax.snack.lowcode.api.vo.LowcodeDataTypeVO;
import org.jax.snack.lowcode.biz.converter.LowcodeDataTypeConverter;
import org.jax.snack.lowcode.biz.entity.LowcodeMetaSchema;
import org.jax.snack.lowcode.biz.mapper.LowcodeMetaSchemaMapper;
import org.jax.snack.lowcode.biz.repository.LowcodeDataTypeRepository;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;

/**
 * 数据类型服务.
 * <p>
 * 提供元数据模板管理功能.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeDataTypeService {

	private final LowcodeDataTypeRepository dataTypeRepository;

	private final LowcodeDataTypeConverter dataTypeConverter;

	private final LowcodeMetaSchemaMapper metaSchemaMapper;

	/**
	 * 获取所有启用的数据类型.
	 * @return 数据类型 VO 列表
	 */
	public List<LowcodeDataTypeVO> getDataTypes() {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("enabled", Map.of(QueryOperator.EQ.getValue(), true)));
		OrderByCondition orderBy = new OrderByCondition();
		orderBy.setField("sortOrder");
		orderBy.setDirection(SortDirection.ASC.getValue());
		condition.setOrderBy(List.of(orderBy));
		return this.dataTypeRepository.queryListByDsl(condition).stream().map(this.dataTypeConverter::toVo).toList();
	}

	/**
	 * 获取实体模板 Schema.
	 * @return JSON Schema
	 */
	public JsonNode getEntityTemplate() {
		return getTemplateByType("entity");
	}

	/**
	 * 获取字段模板 Schema.
	 * @return JSON Schema
	 */
	public JsonNode getFieldTemplate() {
		return getTemplateByType("field");
	}

	/**
	 * 根据类型获取模板.
	 * @param metaType 模板类型
	 * @return JSON Schema
	 */
	public JsonNode getTemplateByType(String metaType) {
		LowcodeMetaSchema metaSchema = this.metaSchemaMapper.selectByMetaType(metaType);
		if (metaSchema == null) {
			throw new IllegalArgumentException("未找到模板: " + metaType);
		}
		return metaSchema.getSchemaJson();
	}

}
