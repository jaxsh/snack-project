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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.model.SchemaChange;
import org.jax.snack.lowcode.biz.model.SchemaChangeType;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Schema 变更检测服务.
 * <p>
 * 对比两个 JSON Schema 定义，检测字段层面的变更.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchemaChangeDetector {

	private final DataTypeManager dataTypeManager;

	/**
	 * 对比新旧 Schema 检测变更.
	 * @param newSchema 新的 Schema (Target)
	 * @param oldSchema 旧的 Schema (Base)，如果为 null 则视为新建表
	 * @return 变更列表
	 */
	public List<SchemaChange> detectChanges(JsonNode newSchema, JsonNode oldSchema) {
		List<SchemaChange> changes = new ArrayList<>();

		// 1. 无旧 Schema，视为全量新增表
		if (oldSchema == null || oldSchema.isMissingNode()) {
			SchemaChange addTable = new SchemaChange();
			addTable.setType(SchemaChangeType.ADD_TABLE);
			changes.add(addTable);
			return changes;
		}

		// 2. 提取字段定义（按 fieldId 索引）
		Map<String, ColumnInfo> newColumnsById = getColumnsByFieldId(newSchema);
		Map<String, ColumnInfo> oldColumnsById = getColumnsByFieldId(oldSchema);

		// 3. 对比检测变更
		Set<String> processedOldIds = new HashSet<>();

		for (Map.Entry<String, ColumnInfo> entry : newColumnsById.entrySet()) {
			String fieldId = entry.getKey();
			ColumnInfo newCol = entry.getValue();

			if (oldColumnsById.containsKey(fieldId)) {
				// 相同 fieldId 存在于旧 Schema
				ColumnInfo oldCol = oldColumnsById.get(fieldId);
				processedOldIds.add(fieldId);

				// 检测列名变更 (RENAME_COLUMN)
				if (!newCol.name.equals(oldCol.name)) {
					String colType = this.dataTypeManager.resolveSqlType(newCol.type, newCol.length, newCol.scale);
					changes.add(SchemaChange.renameColumn(oldCol.name, newCol.name, colType));
				}

				// 检测其他属性变更
				detectColumnChanges(newCol.name, newCol, oldCol, changes);
			}
			else {
				// 新字段 (新增)
				String colType = this.dataTypeManager.resolveSqlType(newCol.type, newCol.length, newCol.scale);
				changes.add(SchemaChange.addColumn(newCol.name, colType, newCol.nullable, newCol.defaultValue,
						newCol.remarks));
			}
		}

		// 4. 检测删除列（旧 Schema 中有但新 Schema 中没有的 fieldId）
		for (Map.Entry<String, ColumnInfo> entry : oldColumnsById.entrySet()) {
			if (!processedOldIds.contains(entry.getKey())) {
				changes.add(SchemaChange.dropColumn(entry.getValue().name));
			}
		}

		// 5. 检测联合索引变更
		detectIndexChanges(newSchema, oldSchema, changes);

		return changes;
	}

	/**
	 * 按 x-field-id 索引提取字段信息.
	 * @param schema Schema JSON definition
	 * @return 字段ID到列信息的映射
	 */
	private Map<String, ColumnInfo> getColumnsByFieldId(JsonNode schema) {
		Map<String, ColumnInfo> columns = new HashMap<>();
		JsonNode properties = schema.path("properties");
		if (properties.isMissingNode()) {
			return columns;
		}

		var fieldNames = properties.properties();
		for (var fieldEntry : fieldNames) {
			String fieldName = fieldEntry.getKey();
			JsonNode fieldSchema = fieldEntry.getValue();

			String fieldId = fieldSchema.path("x-field-id").asString(null);
			if (fieldId == null) {
				// 没有 fieldId，使用列名作为 fallback
				JsonNode xDatabase = fieldSchema.path("x-database");
				fieldId = xDatabase.path("column").asString(fieldName);
			}

			JsonNode xDatabase = fieldSchema.path("x-database");
			ColumnInfo col = new ColumnInfo();
			col.name = xDatabase.path("column").asString(fieldName);
			col.type = xDatabase.path("type").asString("varchar");
			col.length = xDatabase.path("length").asInt(0);
			col.scale = xDatabase.path("scale").asInt(0);
			col.nullable = xDatabase.path("nullable").asBoolean(true);
			col.defaultValue = xDatabase.path("default").asString(null);
			col.remarks = xDatabase.path("comment").asString(null);
			col.index = xDatabase.path("index").asBoolean(false);
			col.unique = xDatabase.path("unique").asBoolean(false);

			columns.put(fieldId, col);
		}

		return columns;
	}

	private void detectColumnChanges(String colName, ColumnInfo newCol, ColumnInfo oldCol, List<SchemaChange> changes) {
		// 1. 检测类型/长度/精度变更 (直接比对属性)
		boolean typeChanged = !newCol.type.equalsIgnoreCase(oldCol.type);
		boolean lengthChanged = newCol.length != oldCol.length;
		boolean scaleChanged = newCol.scale != oldCol.scale;

		if (typeChanged || lengthChanged || scaleChanged) {
			String oldTypeStr = this.dataTypeManager.resolveSqlType(oldCol.type, oldCol.length, oldCol.scale);
			String newTypeStr = this.dataTypeManager.resolveSqlType(newCol.type, newCol.length, newCol.scale);
			changes.add(SchemaChange.modifyType(colName, oldTypeStr, newTypeStr));
		}

		// 2. 检测可空约束变更 (排除主键)
		if (newCol.nullable != oldCol.nullable) {
			String newTypeStr = this.dataTypeManager.resolveSqlType(newCol.type, newCol.length, newCol.scale);
			changes.add(SchemaChange.modifyNullable(colName, newCol.nullable, newTypeStr));
		}

		// 3. 检测默认值变更
		if (!equalsDefault(newCol.defaultValue, oldCol.defaultValue)) {
			changes.add(SchemaChange.modifyDefault(colName, oldCol.defaultValue, newCol.defaultValue));
		}

		// 4. 检测索引变更
		boolean newHasIndex = newCol.index || newCol.unique;
		boolean oldHasIndex = oldCol.index || oldCol.unique;
		if (newHasIndex && !oldHasIndex) {
			String indexName = "idx_" + colName;
			changes.add(SchemaChange.addIndex(colName, indexName, newCol.unique));
		}
		else if (!newHasIndex && oldHasIndex) {
			String indexName = "idx_" + colName;
			changes.add(SchemaChange.dropIndex(indexName));
		}
		else if (newHasIndex && newCol.unique != oldCol.unique) {
			// unique 变更需要先删后加
			String indexName = "idx_" + colName;
			changes.add(SchemaChange.dropIndex(indexName));
			changes.add(SchemaChange.addIndex(colName, indexName, newCol.unique));
		}
	}

	private boolean equalsDefault(String a, String b) {
		if (!StringUtils.hasText(a) && !StringUtils.hasText(b)) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	private void detectIndexChanges(JsonNode newSchema, JsonNode oldSchema, List<SchemaChange> changes) {
		Map<String, IndexInfo> newIndexes = getIndexes(newSchema);
		Map<String, IndexInfo> oldIndexes = getIndexes(oldSchema);

		// 检测新增/修改的索引
		for (Map.Entry<String, IndexInfo> entry : newIndexes.entrySet()) {
			String indexName = entry.getKey();
			IndexInfo newIdx = entry.getValue();

			if (oldIndexes.containsKey(indexName)) {
				IndexInfo oldIdx = oldIndexes.get(indexName);
				// 列组合或 unique 变化, 需要先删后加
				if (!newIdx.columns.equals(oldIdx.columns) || newIdx.unique != oldIdx.unique) {
					changes.add(SchemaChange.dropIndex(indexName));
					changes.add(SchemaChange.addCompositeIndex(newIdx.columns, indexName, newIdx.unique));
				}
			}
			else {
				// 新增索引
				changes.add(SchemaChange.addCompositeIndex(newIdx.columns, indexName, newIdx.unique));
			}
		}

		// 检测删除的索引
		for (String indexName : oldIndexes.keySet()) {
			if (!newIndexes.containsKey(indexName)) {
				changes.add(SchemaChange.dropIndex(indexName));
			}
		}
	}

	private Map<String, IndexInfo> getIndexes(JsonNode schema) {
		Map<String, IndexInfo> indexes = new HashMap<>();
		JsonNode xLowcode = schema.path("x-lowcode");
		JsonNode indexesNode = xLowcode.path("indexes");

		if (indexesNode.isMissingNode() || !indexesNode.isArray()) {
			return indexes;
		}

		for (JsonNode idxNode : indexesNode) {
			String name = idxNode.path("name").asString(null);
			if (name == null) {
				continue;
			}

			IndexInfo info = new IndexInfo();
			info.name = name;
			info.unique = idxNode.path("unique").asBoolean(false);

			JsonNode columnsNode = idxNode.path("columns");
			if (columnsNode.isArray()) {
				for (JsonNode colNode : columnsNode) {
					info.columns.add(colNode.asString());
				}
			}

			indexes.put(name, info);
		}

		return indexes;
	}

	private static class ColumnInfo {

		String name;

		String type;

		int length;

		int scale;

		boolean nullable;

		String defaultValue;

		String remarks;

		boolean index;

		boolean unique;

	}

	private static class IndexInfo {

		String name;

		List<String> columns = new ArrayList<>();

		boolean unique;

	}

}
