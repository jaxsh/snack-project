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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.mybatisplus.context.TableContextHolder;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;
import org.jax.snack.framework.mybatisplus.query.QueryWrapperBuilder;
import org.jax.snack.lowcode.biz.mapper.DynamicCrudMapper;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 动态 CRUD 服务.
 * <p>
 * 使用 MyBatis Plus Wrapper 处理查询/更新，手动处理审计字段.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCrudService {

	private final SchemaService schemaService;

	private final DynamicCrudMapper dynamicCrudMapper;

	private final DataTransformer dataTransformer;

	private final DynamicValidator validator;

	private final SystemFieldManager systemFieldManager;

	/**
	 * 创建数据.
	 * @param schemaName Schema 名称
	 * @param data 数据
	 */
	@Transactional
	public void create(String schemaName, Map<String, Object> data) {
		// 1. 数据校验
		this.validator.validate(schemaName, data);

		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		// 2. 转换为数据库列名映射并补齐审计字段
		Map<String, Object> storageData = this.dataTransformer.toStorage(data, fields);
		this.systemFieldManager.fillSystemParams(storageData, false);

		// 3. 执行插入
		try {
			TableContextHolder.setTableName(metadata.getTableName());
			this.dynamicCrudMapper.dynamicInsert(storageData);

			Object idObj = storageData.get(SystemFieldManager.COLUMN_ID);
			Long id = (idObj instanceof Number n) ? n.longValue() : null;

			if (id != null) {
				data.put(SystemFieldManager.FIELD_ID, id);
			}

			log.info("创建 {} 记录成功, id={}", schemaName, id);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	/**
	 * 更新数据.
	 * @param schemaName Schema 名称
	 * @param id 主键
	 * @param data 数据
	 */
	@Transactional
	public void update(String schemaName, Long id, Map<String, Object> data) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		try {
			TableContextHolder.setTableName(metadata.getTableName());

			// 构建 UpdateWrapper 实现 Patch 更新
			UpdateWrapper<Void> wrapper = new UpdateWrapper<>();
			wrapper.eq(SystemFieldManager.COLUMN_ID, id);

			// 转换并设置更新字段
			Map<String, Object> storageData = this.dataTransformer.toStorage(data, fields);
			this.systemFieldManager.fillSystemParams(storageData, true);

			storageData.forEach((column, value) -> {
				if (value != null && !SystemFieldManager.COLUMN_ID.equals(column)) {
					wrapper.set(column, value);
				}
			});

			int rows = this.dynamicCrudMapper.dynamicUpdate(wrapper);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	/**
	 * 删除数据 (物理删除).
	 * @param schemaName Schema 名称
	 * @param id 主键
	 */
	@Transactional
	public void delete(String schemaName, Long id) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);

		try {
			TableContextHolder.setTableName(metadata.getTableName());

			// 物理删除
			QueryWrapper<Void> wrapper = new QueryWrapper<>();
			wrapper.eq(SystemFieldManager.COLUMN_ID, id);

			int rows = this.dynamicCrudMapper.dynamicDelete(wrapper);
		}
		finally {
			TableContextHolder.clear();
		}
	}

	/**
	 * 分页查询.
	 * @param schemaName Schema 名称
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	public PageResult<Map<String, Object>> queryPage(String schemaName, QueryCondition condition) {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);

		// 1. 构建字段映射
		Map<String, String> fieldMapping = new HashMap<>();
		for (FieldDefinition field : fields) {
			fieldMapping.put(field.getFieldName(), field.getDbColumn());
		}

		// 注入系统字段映射，确保 ID 和 审计字段 也能参与查询/排序
		for (SystemFieldManager.SystemField sysField : this.systemFieldManager.getSystemFields()) {
			fieldMapping.putIfAbsent(sysField.fieldName(), sysField.columnName());
		}

		QueryWrapper<Void> wrapper = QueryWrapperBuilder.build(condition, fieldMapping);

		try {
			TableContextHolder.setTableName(metadata.getTableName());
			String selectColumns = this.dataTransformer.buildSelectColumns(fields);

			if (condition.getSize() != null && condition.getSize() > 0) {
				Page<Map<String, Object>> page = new Page<>(
						(condition.getCurrent() != null) ? condition.getCurrent() : 1, condition.getSize());

				IPage<Map<String, Object>> pageResult = this.dynamicCrudMapper.dynamicSelectPage(page, selectColumns,
						wrapper);

				List<Map<String, Object>> records = this.dataTransformer.toDisplayList(pageResult.getRecords(), fields);
				return new PageResult<>(records, pageResult.getTotal(), pageResult.getSize(), pageResult.getCurrent(),
						pageResult.getPages());
			}
			else {
				List<Map<String, Object>> list = this.dynamicCrudMapper.dynamicSelectList(selectColumns, wrapper);
				List<Map<String, Object>> records = this.dataTransformer.toDisplayList(list, fields);
				return new PageResult<>(records, records.size(), records.size(), 1L, 1L);
			}
		}
		finally {
			TableContextHolder.clear();
		}
	}

}
