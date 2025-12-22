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

package org.jax.snack.lowcode.biz.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Schema 变更信息.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class SchemaChange {

	/**
	 * 变更类型.
	 */
	private SchemaChangeType type;

	/**
	 * 列名.
	 */
	private String columnName;

	/**
	 * 列类型 (用于 ADD_COLUMN).
	 */
	private String columnType;

	/**
	 * 旧值 (用于修改类操作).
	 */
	private String oldValue;

	/**
	 * 新值 (用于修改类操作).
	 */
	private String newValue;

	/**
	 * 是否可空 (用于 MODIFY_NULLABLE).
	 */
	private Boolean nullable;

	/**
	 * 默认值 (用于 MODIFY_DEFAULT).
	 */
	private String defaultValue;

	/**
	 * 索引名 (用于 ADD_INDEX / DROP_INDEX).
	 */
	private String indexName;

	/**
	 * 是否唯一索引.
	 */
	private Boolean unique;

	/**
	 * 注释.
	 */
	private String remarks;

	/**
	 * 索引列列表 (用于联合索引).
	 */
	private List<String> indexColumns;

	/**
	 * 创建新增列变更.
	 * @param columnName 列名
	 * @param columnType 列类型
	 * @param nullable 是否可空
	 * @param defaultValue 默认值
	 * @param remarks 注释
	 * @return 变更对象
	 */
	public static SchemaChange addColumn(String columnName, String columnType, Boolean nullable, String defaultValue,
			String remarks) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.ADD_COLUMN);
		change.setColumnName(columnName);
		change.setColumnType(columnType);
		change.setNullable(nullable);
		change.setDefaultValue(defaultValue);
		change.setRemarks(remarks);
		return change;
	}

	/**
	 * 创建删除列变更.
	 * @param columnName 列名
	 * @return 变更对象
	 */
	public static SchemaChange dropColumn(String columnName) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.DROP_COLUMN);
		change.setColumnName(columnName);
		return change;
	}

	/**
	 * 创建修改类型变更.
	 * @param columnName 列名
	 * @param oldType 旧类型
	 * @param newType 新类型
	 * @return 变更对象
	 */
	public static SchemaChange modifyType(String columnName, String oldType, String newType) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.MODIFY_COLUMN_TYPE);
		change.setColumnName(columnName);
		change.setOldValue(oldType);
		change.setNewValue(newType);
		return change;
	}

	/**
	 * 创建修改可空约束变更.
	 * @param columnName 列名
	 * @param nullable 是否可空
	 * @param columnType 列类型
	 * @return 变更对象
	 */
	public static SchemaChange modifyNullable(String columnName, boolean nullable, String columnType) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.MODIFY_NULLABLE);
		change.setColumnName(columnName);
		change.setNullable(nullable);
		change.setColumnType(columnType);
		return change;
	}

	/**
	 * 创建修改默认值变更.
	 * @param columnName 列名
	 * @param oldDefault 旧默认值
	 * @param newDefault 新默认值
	 * @return 变更对象
	 */
	public static SchemaChange modifyDefault(String columnName, String oldDefault, String newDefault) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.MODIFY_DEFAULT);
		change.setColumnName(columnName);
		change.setOldValue(oldDefault);
		change.setNewValue(newDefault);
		change.setDefaultValue(newDefault);
		return change;
	}

	/**
	 * 创建新增索引变更 (单列索引).
	 * @param columnName 列名
	 * @param indexName 索引名
	 * @param unique 是否唯一
	 * @return 变更对象
	 */
	public static SchemaChange addIndex(String columnName, String indexName, boolean unique) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.ADD_INDEX);
		change.setColumnName(columnName);
		change.setIndexColumns(List.of(columnName));
		change.setIndexName(indexName);
		change.setUnique(unique);
		return change;
	}

	/**
	 * 创建新增索引变更 (联合索引).
	 * @param columns 列名列表
	 * @param indexName 索引名
	 * @param unique 是否唯一
	 * @return 变更对象
	 */
	public static SchemaChange addCompositeIndex(List<String> columns, String indexName, boolean unique) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.ADD_INDEX);
		change.setIndexColumns(columns);
		change.setIndexName(indexName);
		change.setUnique(unique);
		return change;
	}

	/**
	 * 创建删除索引变更.
	 * @param indexName 索引名
	 * @return 变更对象
	 */
	public static SchemaChange dropIndex(String indexName) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.DROP_INDEX);
		change.setIndexName(indexName);
		return change;
	}

	/**
	 * 创建重命名列变更.
	 * @param oldColumnName 旧列名
	 * @param newColumnName 新列名
	 * @param columnType 列类型
	 * @return 变更对象
	 */
	public static SchemaChange renameColumn(String oldColumnName, String newColumnName, String columnType) {
		SchemaChange change = new SchemaChange();
		change.setType(SchemaChangeType.RENAME_COLUMN);
		change.setColumnName(newColumnName);
		change.setOldValue(oldColumnName);
		change.setNewValue(newColumnName);
		change.setColumnType(columnType);
		return change;
	}

}
