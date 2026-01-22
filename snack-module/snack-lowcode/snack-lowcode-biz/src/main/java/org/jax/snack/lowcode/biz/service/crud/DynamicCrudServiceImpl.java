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

package org.jax.snack.lowcode.biz.service.crud;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.mybatisplus.context.TableContextHolder;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;
import org.jax.snack.framework.mybatisplus.query.WrapperBuilder;
import org.jax.snack.lowcode.api.service.DynamicCrudService;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.repository.DynamicCrudRepository;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 动态 CRUD 服务实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudServiceImpl implements DynamicCrudService {

	private final SchemaService schemaService;

	private final DynamicCrudRepository dynamicCrudRepository;

	private final DataTransformer dataTransformer;

	private final DynamicValidator validator;

	private final SystemFieldManager systemFieldManager;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(String schemaName, Map<String, Object> data) {
		this.validator.validate(schemaName, data);

		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		Map<String, Object> storageData = this.dataTransformer.toStorage(data, fields);
		this.systemFieldManager.fillSystemParams(storageData, false);

		try {
			TableContextHolder.setTableName(metadata.getTableName());
			this.dynamicCrudRepository.insert(storageData);

			Object idObj = storageData.get(SystemFieldManager.COLUMN_ID);
			Long id = (idObj instanceof Number n) ? n.longValue() : null;

			if (id != null) {
				data.put(SystemFieldManager.FIELD_ID, id);
			}

			log.info("Record created successfully for {}, id={}", schemaName, id);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(String schemaName, Long id, Map<String, Object> data) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		Map<String, String> fieldMapping = buildFieldMapping(fields);

		try {
			TableContextHolder.setTableName(metadata.getTableName());

			Map<String, Object> storageData = this.dataTransformer.toStorage(data, fields);
			this.systemFieldManager.fillSystemParams(storageData, true);
			storageData.remove(SystemFieldManager.COLUMN_ID);

			WhereCondition condition = WhereCondition.builder().eq(SystemFieldManager.FIELD_ID, id).build();

			UpdateWrapper<Void> wrapper = WrapperBuilder.update(storageData, condition, fieldMapping);
			this.dynamicCrudRepository.update(wrapper);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(String schemaName, List<Long> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}

		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		Map<String, String> fieldMapping = buildFieldMapping(fields);

		try {
			TableContextHolder.setTableName(metadata.getTableName());

			WhereCondition condition = WhereCondition.builder().in(SystemFieldManager.FIELD_ID, ids).build();

			QueryWrapper<Void> wrapper = WrapperBuilder.where(condition, fieldMapping);
			this.dynamicCrudRepository.delete(wrapper);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	@Override
	public Optional<Map<String, Object>> getById(String schemaName, Long id) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);

		Map<String, String> fieldMapping = buildFieldMapping(fields);

		WhereCondition condition = WhereCondition.builder().eq(SystemFieldManager.FIELD_ID, id).build();

		QueryWrapper<Void> wrapper = WrapperBuilder.where(condition, fieldMapping);

		try {
			TableContextHolder.setTableName(metadata.getTableName());
			String selectColumns = this.dataTransformer.buildSelectColumns(fields);

			List<Map<String, Object>> list = this.dynamicCrudRepository.selectList(selectColumns, wrapper);
			if (list.isEmpty()) {
				return Optional.empty();
			}

			List<Map<String, Object>> records = this.dataTransformer.toDisplayList(list, fields);
			return Optional.of(records.get(0));
		}
		finally {
			TableContextHolder.clear();
		}
	}

	@Override
	public PageResult<Map<String, Object>> queryPage(String schemaName, QueryCondition condition) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);

		Map<String, String> fieldMapping = buildFieldMapping(fields);

		QueryWrapper<Void> wrapper = WrapperBuilder.query(condition, fieldMapping);

		try {
			TableContextHolder.setTableName(metadata.getTableName());
			String selectColumns = this.dataTransformer.buildSelectColumns(fields);

			if (!ObjectUtils.isEmpty(condition.getSize())) {
				Page<Map<String, Object>> page = new Page<>(
						(condition.getCurrent() != null) ? condition.getCurrent() : 1, condition.getSize());

				IPage<Map<String, Object>> pageResult = this.dynamicCrudRepository.selectPage(page, selectColumns,
						wrapper);

				List<Map<String, Object>> records = this.dataTransformer.toDisplayList(pageResult.getRecords(), fields);
				return new PageResult<>(records, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(),
						pageResult.getPages());
			}
			else {
				List<Map<String, Object>> list = this.dynamicCrudRepository.selectList(selectColumns, wrapper);
				List<Map<String, Object>> records = this.dataTransformer.toDisplayList(list, fields);
				return new PageResult<>(records, records.size(), records.size(), 1L, 1L);
			}
		}
		finally {
			TableContextHolder.clear();
		}
	}

	private Map<String, String> buildFieldMapping(List<FieldDefinition> fields) {
		Map<String, String> fieldMapping = new HashMap<>();
		for (FieldDefinition field : fields) {
			fieldMapping.put(field.getFieldName(), field.getDbColumn());
		}
		for (SystemFieldManager.SystemField sysField : this.systemFieldManager.getSystemFields()) {
			fieldMapping.putIfAbsent(sysField.fieldName(), sysField.columnName());
		}
		return fieldMapping;
	}

}
