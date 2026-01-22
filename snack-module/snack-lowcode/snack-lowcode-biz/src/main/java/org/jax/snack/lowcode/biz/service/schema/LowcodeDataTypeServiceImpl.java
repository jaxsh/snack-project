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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.lowcode.api.service.LowcodeDataTypeService;
import org.jax.snack.lowcode.api.vo.LowcodeDataTypeVO;
import org.jax.snack.lowcode.biz.converter.LowcodeDataTypeConverter;
import org.jax.snack.lowcode.biz.entity.LowcodeDataType;
import org.jax.snack.lowcode.biz.entity.LowcodeMetaSchema;
import org.jax.snack.lowcode.biz.repository.LowcodeDataTypeRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeMetaSchemaRepository;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;

/**
 * 数据类型服务实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeDataTypeServiceImpl implements LowcodeDataTypeService {

	private final LowcodeDataTypeRepository dataTypeRepository;

	private final LowcodeDataTypeConverter dataTypeConverter;

	private final LowcodeMetaSchemaRepository metaSchemaRepository;

	@Override
	public List<LowcodeDataTypeVO> getDataTypes() {
		QueryCondition condition = QueryCondition.builder()
			.eq(LowcodeDataType.Fields.enabled, true)
			.orderByAsc(LowcodeDataType.Fields.sortOrder)
			.build();
		return this.dataTypeRepository.queryListByDsl(condition).stream().map(this.dataTypeConverter::toVo).toList();
	}

	@Override
	public JsonNode getEntityTemplate() {
		return getTemplateByType("entity");
	}

	@Override
	public JsonNode getFieldTemplate() {
		return getTemplateByType("field");
	}

	private JsonNode getTemplateByType(String metaType) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeMetaSchema.Fields.metaType, metaType).build();
		List<LowcodeMetaSchema> list = this.metaSchemaRepository.queryListByDsl(condition);
		if (list.isEmpty()) {
			throw new IllegalArgumentException("Template not found: " + metaType);
		}
		return list.get(0).getSchemaJson();
	}

}
