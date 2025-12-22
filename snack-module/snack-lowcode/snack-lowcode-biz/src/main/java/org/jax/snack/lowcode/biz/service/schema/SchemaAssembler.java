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

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO.FieldDTO;
import org.jax.snack.lowcode.biz.entity.LowcodeDataType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Schema 组装器.
 * <p>
 * 将前端表单数据转换为 JSON Schema 格式.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaAssembler {

	private static final String PROPERTY_TYPE = "type";

	private final JsonMapper jsonMapper;

	private final DataTypeManager dataTypeManager;

	private final SystemFieldManager systemFieldManager;

	/**
	 * 将创建请求转换为 JSON Schema.
	 * @param request 创建请求
	 * @return JSON Schema
	 */
	public JsonNode toJsonSchema(LowcodeSchemaDTO request) {
		ObjectNode schema = this.jsonMapper.createObjectNode();

		// 根节点
		schema.put("$schema", "http://json-schema.org/draft-07/schema#");
		schema.put(PROPERTY_TYPE, "object");

		// x-lowcode 扩展
		ObjectNode xLowcode = schema.putObject("x-lowcode");
		xLowcode.put("entityName", request.getSchemaName());
		xLowcode.put("tableName", request.getTableName());
		xLowcode.put("label", request.getLabel());
		if (StringUtils.hasText(request.getDescription())) {
			xLowcode.put("description", request.getDescription());
		}

		ObjectNode properties = schema.putObject("properties");

		// 添加系统字段 (ID + Audit)
		addSystemFields(properties);

		// 添加用户定义字段
		ArrayNode required = schema.putArray("required");
		for (FieldDTO field : request.getFields()) {
			ObjectNode fieldSchema = properties.putObject(field.getFieldName());
			buildFieldSchema(fieldSchema, field);

			// 必填字段
			if (Boolean.FALSE.equals(field.getNullable())) {
				required.add(field.getFieldName());
			}
		}

		// 添加联合索引
		addIndexes(xLowcode, request);

		return schema;
	}

	private void addSystemFields(ObjectNode properties) {
		for (SystemFieldManager.SystemField sysCol : this.systemFieldManager.getSystemFields()) {
			String fieldName = sysCol.fieldName();

			ObjectNode field = properties.putObject(fieldName);

			// 查找类型配置获取 JSON Schema 类型
			LowcodeDataType config = this.dataTypeManager.getConfig(sysCol.logicType());
			String jsonType = (config != null) ? config.getJsonSchemaType() : "string";

			field.put(PROPERTY_TYPE, jsonType);
			field.put("title", sysCol.remarks());

			// 对于非主键的系统字段，前端只读
			if (!sysCol.isPrimaryKey()) {
				field.put("readOnly", true);
			}

			ObjectNode xDatabase = field.putObject("x-database");
			xDatabase.put("column", sysCol.columnName());
			xDatabase.put("type", sysCol.sqlType());
			xDatabase.put("system", sysCol.isSystem());

			// 添加长度（如有）
			if (sysCol.length() != null) {
				xDatabase.put("length", sysCol.length());
			}

			if (sysCol.isPrimaryKey()) {
				xDatabase.put("primaryKey", true);
			}
			if (sysCol.isAutoIncrement()) {
				xDatabase.put("autoIncrement", true);
			}
			if (sysCol.defaultValueComputed() != null) {
				xDatabase.put("defaultValueComputed", sysCol.defaultValueComputed());
			}

			ObjectNode xUi = field.putObject("x-ui");
			xUi.put("hidden", true);
		}
	}

	private void addIndexes(ObjectNode xLowcode, LowcodeSchemaDTO request) {
		if (CollectionUtils.isEmpty(request.getIndexes())) {
			return;
		}

		ArrayNode indexesArray = xLowcode.putArray("indexes");
		for (LowcodeSchemaDTO.IndexDTO idx : request.getIndexes()) {
			ObjectNode indexNode = indexesArray.addObject();

			// 索引名: 用户指定或自动生成
			String indexName = StringUtils.hasText(idx.getIndexName()) ? idx.getIndexName() : generateIndexName(idx);
			indexNode.put("name", indexName);

			// 列名列表
			ArrayNode columnsArray = indexNode.putArray("columns");
			for (String column : idx.getColumns()) {
				columnsArray.add(column);
			}

			// 是否唯一
			if (Boolean.TRUE.equals(idx.getUnique())) {
				indexNode.put("unique", true);
			}
		}
	}

	private String generateIndexName(LowcodeSchemaDTO.IndexDTO idx) {
		String prefix = Boolean.TRUE.equals(idx.getUnique()) ? "uk_" : "idx_";
		return prefix + String.join("_", idx.getColumns());
	}

	private void buildFieldSchema(ObjectNode fieldSchema, FieldDTO field) {
		// 字段唯一标识
		String fieldId = StringUtils.hasText(field.getFieldId()) ? field.getFieldId() : generateFieldId();
		fieldSchema.put("x-field-id", fieldId);

		// 查找类型配置
		LowcodeDataType config = this.dataTypeManager.getConfig(field.getLogicType());
		String jsonType = (config != null) ? config.getJsonSchemaType() : "string";
		String dbType = (config != null) ? config.getDbType() : field.getLogicType();

		// JSON Schema 类型
		fieldSchema.put(PROPERTY_TYPE, jsonType);
		fieldSchema.put("title", field.getTitle());

		// x-database 扩展
		ObjectNode xDatabase = fieldSchema.putObject("x-database");
		xDatabase.put("column", field.getDbColumn());
		xDatabase.put(PROPERTY_TYPE, dbType);

		if (field.getLength() != null) {
			xDatabase.put("length", field.getLength());
		}
		if (field.getScale() != null) {
			xDatabase.put("scale", field.getScale());
		}
		if (field.getNullable() != null) {
			xDatabase.put("nullable", field.getNullable());
		}
		if (StringUtils.hasText(field.getDefaultValue())) {
			xDatabase.put("default", field.getDefaultValue());
		}
		if (StringUtils.hasText(field.getComment())) {
			xDatabase.put("comment", field.getComment());
		}
		if (Boolean.TRUE.equals(field.getIndex())) {
			xDatabase.put("index", true);
		}
		if (Boolean.TRUE.equals(field.getUnique())) {
			xDatabase.put("unique", true);
		}

		// x-list 扩展
		ObjectNode xList = fieldSchema.putObject("x-list");
		xList.put("visible", true);
	}

	/**
	 * 生成字段唯一标识.
	 * @return 字段ID
	 */
	private String generateFieldId() {
		return "f_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

}
