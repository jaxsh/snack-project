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
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.jax.snack.lowcode.biz.exception.SchemaNotFoundException;
import org.jax.snack.lowcode.biz.mapper.LowcodeSchemaMapper;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.options.OptionItem;
import org.jax.snack.lowcode.biz.options.OptionsResolver;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * Schema 管理服务.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaService {

	private final LowcodeSchemaMapper schemaMapper;

	private final ObjectProvider<OptionsResolver> optionsResolverProvider;

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
			LowcodeSchema entity = this.schemaMapper.selectPublishedByName(name);
			if (entity == null) {
				throw new SchemaNotFoundException(name + " (published)");
			}
			return entity.getSchemaJson();
		});
	}

	/**
	 * 根据 API 路径获取已发布的 JSON Schema.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return JSON Schema
	 */
	public JsonNode getSchemaByResourcePath(String resourcePath) {
		LowcodeSchema entity = this.schemaMapper.selectPublishedByResourcePath(resourcePath);
		if (entity == null) {
			throw new SchemaNotFoundException("resourcePath: " + resourcePath + " (published)");
		}
		// 使用 schemaName 作为缓存 key
		return this.schemaCache.computeIfAbsent(entity.getSchemaName(), (k) -> entity.getSchemaJson());
	}

	/**
	 * 根据 API 路径获取已发布的 Schema 名称.
	 * @param resourcePath API 路径 (kebab-case)
	 * @return Schema 名称
	 */
	public String getSchemaNameByResourcePath(String resourcePath) {
		LowcodeSchema entity = this.schemaMapper.selectPublishedByResourcePath(resourcePath);
		if (entity == null) {
			throw new SchemaNotFoundException("resourcePath: " + resourcePath + " (published)");
		}
		return entity.getSchemaName();
	}

	/**
	 * 从 Schema 中提取元信息.
	 * @param schema JSON Schema
	 * @return Schema 元信息
	 */
	public SchemaMetadata extractMetadata(JsonNode schema) {
		JsonNode lowcode = schema.get("x-lowcode");
		if (lowcode == null) {
			throw new IllegalArgumentException("Schema 缺少 x-lowcode 配置");
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
		if (properties == null) {
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
	 * @param schema JSON Schema
	 * @return 可见字段列表
	 */
	public List<FieldDefinition> extractVisibleFields(JsonNode schema) {
		return extractFields(schema).stream().filter(FieldDefinition::isListVisible).toList();
	}

	/**
	 * 清除 Schema 缓存.
	 * @param schemaName Schema 名称
	 */
	public void clearCache(String schemaName) {
		this.schemaCache.remove(schemaName);
		log.info("清除 Schema 缓存: {}", schemaName);
	}

	/**
	 * 清除所有缓存.
	 */
	public void clearAllCache() {
		this.schemaCache.clear();
		log.info("清除所有 Schema 缓存");
	}

	private Set<String> extractRequiredFields(JsonNode schema) {
		JsonNode required = schema.get("required");
		Set<String> requiredSet = new HashSet<>();
		if (required != null && required.isArray()) {
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

		// 解析 x-database
		if (xDatabase != null) {
			builder.dbColumn(getTextOrNull(xDatabase, "column"));
		}
		else {
			// 无 x-database 配置时，使用 fieldName 作为默认列名
			builder.dbColumn(fieldName);
		}

		// 解析 x-ui
		if (xUi != null) {
			builder.widget(getTextOrNull(xUi, "widget"));
		}

		// 解析 x-options (选项配置)
		JsonNode xOptions = fieldSchema.get("x-options");
		if (xOptions != null) {
			builder.xOptions(xOptions);
		}

		// 解析 x-list
		if (xList != null) {
			builder.listVisible(xList.path("visible").asBoolean(false));
			if (xList.has("width")) {
				builder.width(xList.get("width").asInt());
			}
		}

		return builder.build();
	}

	private String getTextOrNull(JsonNode node, String fieldName) {
		JsonNode child = node.get(fieldName);
		return (child != null && !child.isNull()) ? child.asString() : null;
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
		if (xOptions == null) {
			return List.of();
		}

		return Objects.requireNonNull(this.optionsResolverProvider.getIfAvailable()).resolveOptions(xOptions);
	}

}
