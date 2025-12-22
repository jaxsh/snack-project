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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.options.OptionsResolver;

import org.springframework.stereotype.Component;

/**
 * 数据转换器.
 * <p>
 * 负责 JSON 数据与数据库格式之间的转换，基于 JSON Schema type 进行类型标准化. 纯粹的转换工具，不包含特定字段（如 ID）的硬编码逻辑.
 * </p>
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class DataTransformer {

	private final OptionsResolver optionsResolver;

	/**
	 * JSON 数据转换为数据库存储格式.
	 * @param jsonData JSON 数据 (fieldName -> value)
	 * @param fields 字段定义
	 * @return 数据库格式 (dbColumn -> value)
	 */
	public Map<String, Object> toStorage(Map<String, Object> jsonData, List<FieldDefinition> fields) {
		Map<String, Object> storageData = new HashMap<>();
		fields.stream()
			.filter((field) -> jsonData.containsKey(field.getFieldName()))
			.forEach((field) -> storageData.put(field.getDbColumn(), jsonData.get(field.getFieldName())));
		return storageData;
	}

	/**
	 * 数据库格式转换为 JSON 显示格式.
	 * @param storageData 数据库格式 (fieldName 驼峰格式 -> value, 由 MyBatis MapWrapperFactory 转换)
	 * @param fields 字段定义
	 * @return JSON 格式 (fieldName -> value, 包含 xxxLabel 翻译字段)
	 */
	public Map<String, Object> toDisplay(Map<String, Object> storageData, List<FieldDefinition> fields) {
		Map<String, Object> displayData = new LinkedHashMap<>();

		fields.stream().filter((field) -> storageData.containsKey(field.getFieldName())).forEach((field) -> {
			Object value = storageData.get(field.getFieldName());
			// 根据 JSON Schema type 转换类型
			Object convertedValue = convertToJavaType(value, field.getType());
			displayData.put(field.getFieldName(), convertedValue);

			// 添加 Label 翻译字段
			if (field.getXOptions() != null) {
				String label = this.optionsResolver.getLabel(field.getXOptions(), convertedValue);
				displayData.put(field.getFieldName() + "Label", label);
			}
		});

		return displayData;
	}

	/**
	 * 批量转换为显示格式.
	 * @param storageDataList 数据库格式列表
	 * @param fields 字段定义
	 * @return JSON 格式列表
	 */
	public List<Map<String, Object>> toDisplayList(List<Map<String, Object>> storageDataList,
			List<FieldDefinition> fields) {
		return storageDataList.stream().map((data) -> toDisplay(data, fields)).toList();
	}

	/**
	 * 构建查询列.
	 * @param fields 字段定义
	 * @return SELECT 列字符串
	 */
	public String buildSelectColumns(List<FieldDefinition> fields) {
		return fields.stream().map(FieldDefinition::getDbColumn).collect(Collectors.joining(", "));
	}

	/**
	 * 提取 ID (安全转换).
	 * @param storageData 数据库返回的数据
	 * @return Long 类型的 ID
	 */
	public Long extractId(Map<String, Object> storageData) {
		Object id = storageData.get(SystemFieldManager.COLUMN_ID);
		return (id instanceof Number n) ? n.longValue() : null;
	}

	/**
	 * 根据 JSON Schema type 转换为标准 Java 类型.
	 * <p>
	 * 与数据库无关，基于 JSON Schema 标准.
	 * </p>
	 * @param value 原始值
	 * @param jsonSchemaType JSON Schema 类型 (integer, number, string, boolean)
	 * @return 转换后的值
	 */
	private Object convertToJavaType(Object value, String jsonSchemaType) {
		if (value == null || jsonSchemaType == null) {
			return value;
		}

		return switch (jsonSchemaType) {
			case "integer" -> convertToLong(value);
			case "number" -> convertToDecimal(value);
			case "string" -> value.toString();
			case "boolean" -> convertToBoolean(value);
			default -> value;
		};
	}

	private Long convertToLong(Object value) {
		if (value instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(value.toString());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private BigDecimal convertToDecimal(Object value) {
		if (value instanceof BigDecimal bd) {
			return bd;
		}
		if (value instanceof Number n) {
			return BigDecimal.valueOf(n.doubleValue());
		}
		try {
			return new BigDecimal(value.toString());
		}
		catch (NumberFormatException ex) {
			return null;
		}
	}

	private Boolean convertToBoolean(Object value) {
		if (value instanceof Boolean b) {
			return b;
		}
		if (value instanceof Number n) {
			return n.intValue() != 0;
		}
		return Boolean.parseBoolean(value.toString());
	}

}
