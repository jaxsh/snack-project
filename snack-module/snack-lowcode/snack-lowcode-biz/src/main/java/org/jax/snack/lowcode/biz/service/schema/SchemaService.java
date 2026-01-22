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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;
import org.jax.snack.lowcode.api.enums.SchemaStatus;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.jax.snack.lowcode.biz.exception.SchemaNotFoundException;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.options.OptionItem;
import org.jax.snack.lowcode.biz.options.OptionsResolver;
import org.jax.snack.lowcode.biz.repository.LowcodeSchemaRepository;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Schema 管理服务.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaService {

	private final LowcodeSchemaRepository schemaRepository;

	private final ObjectProvider<OptionsResolver> optionsResolverProvider;

	private final SystemFieldManager systemFieldManager;

	/**
	 * Schema 缓存.
	 */
	private final Map<String, JsonNode> schemaCache = new ConcurrentHashMap<>();

	/**
	 * 根据 Schema 名称获取已发布的 JSON Schema.
	 * @param schemaName Schema 名称
	 * @return JSON Schema
	 */
	public JsonNode getSchema(String schemaName) {
		return this.schemaCache.computeIfAbsent(schemaName, (name) -> {
			LowcodeSchema entity = findPublishedByName(name);
			return entity.getSchemaJson();
		});
	}

	/**
	 * 根据 API 路径获取已发布的 JSON Schema.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return JSON Schema
	 */
	public JsonNode getSchemaByResourcePath(String resourcePath) {
		LowcodeSchema entity = findPublishedByResourcePath(resourcePath);
		return this.schemaCache.computeIfAbsent(entity.getSchemaName(), (k) -> entity.getSchemaJson());
	}

	/**
	 * 根据 API 路径获取已发布的 Schema 名称.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return Schema 名称
	 */
	public String getSchemaNameByResourcePath(String resourcePath) {
		return findPublishedByResourcePath(resourcePath).getSchemaName();
	}

	private LowcodeSchema findPublishedByName(String schemaName) {
		QueryCondition condition = QueryCondition.builder()
			.eq(LowcodeSchema.Fields.schemaName, schemaName)
			.eq(LowcodeSchema.Fields.status, SchemaStatus.PUBLISHED.getCode())
			.build();
		List<LowcodeSchema> list = this.schemaRepository.queryListByDsl(condition);
		if (CollectionUtils.isEmpty(list)) {
			throw new SchemaNotFoundException(schemaName + " (published)");
		}
		return list.get(0);
	}

	private LowcodeSchema findPublishedByResourcePath(String resourcePath) {
		QueryCondition condition = QueryCondition.builder()
			.eq(LowcodeSchema.Fields.resourcePath, resourcePath)
			.eq(LowcodeSchema.Fields.status, SchemaStatus.PUBLISHED.getCode())
			.build();
		List<LowcodeSchema> list = this.schemaRepository.queryListByDsl(condition);
		if (CollectionUtils.isEmpty(list)) {
			throw new SchemaNotFoundException("resourcePath: " + resourcePath + " (published)");
		}
		return list.get(0);
	}

	/**
	 * 从 Schema 中提取元信息.
	 * @param schema JSON Schema
	 * @return Schema 元信息
	 */
	public SchemaMetadata extractMetadata(JsonNode schema) {
		JsonNode lowcode = schema.get("x-lowcode");
		if (lowcode == null) {
			throw new IllegalArgumentException("Schema is missing x-lowcode configuration");
		}
		return SchemaMetadata.builder()
			.entityName(getTextOrNull(lowcode, "entityName"))
			.tableName(getTextOrNull(lowcode, "tableName"))
			.label(getTextOrNull(lowcode, "label"))
			.build();
	}

	/**
	 * 从 Schema 中提取字段定义列表.
	 * @param schema JSON Schema
	 * @return 字段定义列表
	 */
	public List<FieldDefinition> extractFields(JsonNode schema) {
		JsonNode properties = schema.get("properties");
		if (ObjectUtils.isEmpty(properties)) {
			return new ArrayList<>();
		}

		Set<String> requiredFields = extractRequiredFields(schema);
		List<FieldDefinition> fields = new ArrayList<>();

		properties.properties().forEach((entry) -> {
			String fieldName = entry.getKey();
			JsonNode fieldSchema = entry.getValue();
			fields.add(parseFieldDefinition(fieldName, fieldSchema, requiredFields.contains(fieldName)));
		});

		return fields;
	}

	/**
	 * 提取列表可见的字段.
	 * <p>
	 * 自动追加系统审计字段 (createTime, createBy, updateTime, updateBy).
	 * @param schema JSON Schema
	 * @return 可见字段列表 (包含系统字段)
	 */
	public List<FieldDefinition> extractVisibleFields(JsonNode schema) {
		List<FieldDefinition> visibleFields = extractFields(schema).stream()
			.filter(FieldDefinition::isListVisible)
			.toList();

		List<FieldDefinition> allVisible = new ArrayList<>(visibleFields);
		allVisible.addAll(buildSystemFieldDefinitions());

		return allVisible;
	}

	/**
	 * 清除 Schema 缓存.
	 * @param schemaName Schema 名称
	 */
	public void clearCache(String schemaName) {
		this.schemaCache.remove(schemaName);
		log.info("Evicting schema cache: {}", schemaName);
	}

	/**
	 * 清除所有缓存.
	 */
	public void clearAllCache() {
		this.schemaCache.clear();
		log.info("Evicting all schema caches");
	}

	private Set<String> extractRequiredFields(JsonNode schema) {
		JsonNode required = schema.get("required");
		Set<String> requiredSet = new HashSet<>();
		if (!ObjectUtils.isEmpty(required) && required.isArray()) {
			required.forEach((node) -> requiredSet.add(node.asString()));
		}
		return requiredSet;
	}

	private FieldDefinition parseFieldDefinition(String fieldName, JsonNode fieldSchema, boolean isRequired) {
		JsonNode xDatabase = fieldSchema.get("x-database");
		JsonNode xUi = fieldSchema.get("x-ui");
		JsonNode xList = fieldSchema.get("x-list");

		FieldDefinition.FieldDefinitionBuilder builder = FieldDefinition.builder()
			.fieldName(fieldName)
			.type(getTextOrNull(fieldSchema, "type"))
			.label(getTextOrNull(fieldSchema, "title"))
			.required(isRequired);

		if (!ObjectUtils.isEmpty(xDatabase)) {
			builder.dbColumn(getTextOrNull(xDatabase, "column"));
		}
		else {
			builder.dbColumn(fieldName);
		}

		if (!ObjectUtils.isEmpty(xUi)) {
			builder.widget(getTextOrNull(xUi, "widget"));
		}

		JsonNode xOptions = fieldSchema.get("x-options");
		if (!ObjectUtils.isEmpty(xOptions)) {
			builder.xOptions(xOptions);
		}

		if (!ObjectUtils.isEmpty(xList)) {
			builder.listVisible(xList.path("visible").asBoolean(false));
			if (xList.has("width")) {
				builder.width(xList.get("width").asInt());
			}
		}

		return builder.build();
	}

	private String getTextOrNull(JsonNode node, String fieldName) {
		JsonNode child = node.get(fieldName);
		return (!ObjectUtils.isEmpty(child) && !child.isNull()) ? child.asString() : null;
	}

	/**
	 * 获取字段选项列表.
	 * @param schemaName Schema 名称
	 * @param fieldName 字段名
	 * @return 选项列表
	 */
	public List<OptionItem> getFieldOptions(String schemaName, String fieldName) {
		JsonNode schema = getSchema(schemaName);
		JsonNode fieldSchema = schema.path("properties").path(fieldName);

		if (fieldSchema.isMissingNode()) {
			return List.of();
		}

		JsonNode xOptions = fieldSchema.get("x-options");
		if (ObjectUtils.isEmpty(xOptions)) {
			return List.of();
		}

		return Objects.requireNonNull(this.optionsResolverProvider.getIfAvailable()).resolveOptions(xOptions);
	}

	/**
	 * 构建系统字段定义列表.
	 * <p>
	 * 将 SystemFieldManager 中的系统字段转换为 FieldDefinition, 用于查询和显示.
	 * @return 系统字段定义列表 (不包含 id)
	 */
	private List<FieldDefinition> buildSystemFieldDefinitions() {
		return this.systemFieldManager.getSystemFields()
			.stream()
			.filter((sf) -> !sf.isPrimaryKey())
			.map(this::convertToFieldDefinition)
			.toList();
	}

	/**
	 * 将 SystemField 转换为 FieldDefinition.
	 * @param systemField 系统字段
	 * @return FieldDefinition
	 */
	private FieldDefinition convertToFieldDefinition(SystemFieldManager.SystemField systemField) {
		return FieldDefinition.builder()
			.fieldName(systemField.fieldName())
			.dbColumn(systemField.columnName())
			.type(systemField.logicType())
			.label(systemField.remarks())
			.required(false)
			.listVisible(true)
			.build();
	}

}
