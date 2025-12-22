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

package org.jax.snack.lowcode.biz.service.sync;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import liquibase.Liquibase;
import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.AddDefaultValueChange;
import liquibase.change.core.AddNotNullConstraintChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropColumnChange;
import liquibase.change.core.DropDefaultValueChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropNotNullConstraintChange;
import liquibase.change.core.ModifyDataTypeChange;
import liquibase.change.core.RenameColumnChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaChange;
import org.jax.snack.lowcode.biz.model.SchemaChangeType;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.service.schema.DataTypeManager;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 表同步执行器.
 * <p>
 * 使用 Liquibase Java API 创建表或增量变更表结构。
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TableSyncExecutor {

	private final DataSource dataSource;

	private final SchemaService schemaService;

	private final DataTypeManager dataTypeManager;

	/**
	 * 同步表结构.
	 * @param schemaName Schema 名称
	 * @param schemaJson 要同步的 Schema JSON (传入而不是查询)
	 * @param changes 预先检测到的变更列表
	 */
	public void syncTable(String schemaName, JsonNode schemaJson, List<SchemaChange> changes) {
		if (CollectionUtils.isEmpty(changes)) {
			log.info("表结构无变更: {}", schemaName);
			return;
		}

		// 判断是否是新建表
		boolean isCreateTable = changes.size() == 1 && changes.get(0).getType() == SchemaChangeType.ADD_TABLE;

		if (isCreateTable) {
			createTable(schemaName, schemaJson);
		}
		else {
			applyChanges(schemaName, schemaJson, changes);
		}
	}

	/**
	 * 创建新表.
	 * @param schemaName Schema 名称
	 * @param schema Schema JSON definition
	 */
	private void createTable(String schemaName, JsonNode schema) {
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractFields(schema);

		try (Connection conn = this.dataSource.getConnection()) {
			CreateTableChange createTable = new CreateTableChange();
			createTable.setTableName(metadata.getTableName());
			createTable.setRemarks(metadata.getLabel());

			for (FieldDefinition field : fields) {
				JsonNode fieldSchema = schema.path("properties").path(field.getFieldName());
				JsonNode xDatabase = fieldSchema.path("x-database");
				createTable.addColumn(buildColumn(field.getDbColumn(), xDatabase));
			}

			executeChanges(conn, schemaName, List.of(createTable));

			log.info("表创建成功: {}", metadata.getTableName());
		}
		catch (SQLException | LiquibaseException ex) {
			log.error("表创建失败: {}", ex.getMessage(), ex);
			throw new RuntimeException("表创建失败: " + ex.getMessage(), ex);
		}
	}

	/**
	 * 应用增量变更.
	 * @param schemaName Schema 名称
	 * @param schema Schema JSON definition
	 * @param changes 变更列表
	 */
	private void applyChanges(String schemaName, JsonNode schema, List<SchemaChange> changes) {
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		String tableName = metadata.getTableName();

		try (Connection conn = this.dataSource.getConnection()) {
			List<Change> liquibaseChanges = new ArrayList<>();

			for (SchemaChange change : changes) {
				Change lbChange = convertToLiquibaseChange(tableName, change);
				if (lbChange != null) {
					liquibaseChanges.add(lbChange);
					log.info("变更: {} - {}", change.getType(), change.getColumnName());
				}
			}

			if (!liquibaseChanges.isEmpty()) {
				executeChanges(conn, schemaName, liquibaseChanges);
				log.info("表结构变更成功: {}, 变更数: {}", tableName, liquibaseChanges.size());
			}
		}
		catch (SQLException | LiquibaseException ex) {
			log.error("表结构变更失败: {}", ex.getMessage(), ex);
			throw new RuntimeException("表结构变更失败: " + ex.getMessage(), ex);
		}
	}

	private Change convertToLiquibaseChange(String tableName, SchemaChange change) {
		return switch (change.getType()) {
			case ADD_COLUMN -> {
				AddColumnChange addCol = new AddColumnChange();
				addCol.setTableName(tableName);
				AddColumnConfig col = new AddColumnConfig();
				col.setName(change.getColumnName());
				col.setType(change.getColumnType());
				col.setRemarks(change.getRemarks());
				if (change.getDefaultValue() != null) {
					col.setDefaultValue(change.getDefaultValue());
				}
				if (Boolean.FALSE.equals(change.getNullable())) {
					col.setConstraints(new ConstraintsConfig().setNullable(false));
				}
				addCol.addColumn(col);
				yield addCol;
			}
			case DROP_COLUMN -> {
				DropColumnChange dropCol = new DropColumnChange();
				dropCol.setTableName(tableName);
				dropCol.setColumnName(change.getColumnName());
				yield dropCol;
			}
			case MODIFY_COLUMN_TYPE -> {
				ModifyDataTypeChange modType = new ModifyDataTypeChange();
				modType.setTableName(tableName);
				modType.setColumnName(change.getColumnName());
				modType.setNewDataType(change.getNewValue());
				yield modType;
			}
			case MODIFY_NULLABLE -> {
				if (Boolean.TRUE.equals(change.getNullable())) {
					DropNotNullConstraintChange drop = new DropNotNullConstraintChange();
					drop.setTableName(tableName);
					drop.setColumnName(change.getColumnName());
					drop.setColumnDataType(change.getColumnType());
					yield drop;
				}
				else {
					AddNotNullConstraintChange add = new AddNotNullConstraintChange();
					add.setTableName(tableName);
					add.setColumnName(change.getColumnName());
					add.setColumnDataType(change.getColumnType());
					yield add;
				}
			}
			case MODIFY_DEFAULT -> {
				if (change.getNewValue() == null || change.getNewValue().isEmpty()) {
					DropDefaultValueChange drop = new DropDefaultValueChange();
					drop.setTableName(tableName);
					drop.setColumnName(change.getColumnName());
					yield drop;
				}
				else {
					AddDefaultValueChange add = new AddDefaultValueChange();
					add.setTableName(tableName);
					add.setColumnName(change.getColumnName());
					add.setDefaultValue(change.getNewValue());
					yield add;
				}
			}
			case RENAME_COLUMN -> {
				RenameColumnChange rename = new RenameColumnChange();
				rename.setTableName(tableName);
				rename.setOldColumnName(change.getOldValue());
				rename.setNewColumnName(change.getNewValue());
				rename.setColumnDataType(change.getColumnType());
				yield rename;
			}
			case ADD_INDEX -> {
				CreateIndexChange createIndex = new CreateIndexChange();
				createIndex.setTableName(tableName);
				createIndex.setIndexName(change.getIndexName());
				createIndex.setUnique(Boolean.TRUE.equals(change.getUnique()));
				// 支持联合索引: 添加所有列
				for (String colName : change.getIndexColumns()) {
					AddColumnConfig col = new AddColumnConfig();
					col.setName(colName);
					createIndex.addColumn(col);
				}
				yield createIndex;
			}
			case DROP_INDEX -> {
				DropIndexChange dropIndex = new DropIndexChange();
				dropIndex.setTableName(tableName);
				dropIndex.setIndexName(change.getIndexName());
				yield dropIndex;
			}
			default -> null;
		};
	}

	private void executeChanges(Connection conn, String schemaName, List<Change> changes)
			throws LiquibaseException, SQLException {
		Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(conn));
		try (database) {
			String uniqueId = "sync-" + schemaName + "-" + System.currentTimeMillis();
			DatabaseChangeLog changeLog = new DatabaseChangeLog("dynamic/" + schemaName + ".yaml");
			ChangeSet changeSet = new ChangeSet(uniqueId, "lowcode", false, false, "dynamic/" + schemaName, null, null,
					changeLog);

			for (Change change : changes) {
				changeSet.addChange(change);
			}
			changeLog.addChangeSet(changeSet);

			try (Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database)) {
				liquibase.update("");
			}
		}
	}

	private ColumnConfig buildColumn(String columnName, JsonNode xDatabase) {
		ColumnConfig column = new ColumnConfig();
		column.setName(columnName);
		column.setType(buildType(xDatabase));
		column.setRemarks(xDatabase.path("comment").asString(null));

		ConstraintsConfig constraints = new ConstraintsConfig();
		if (xDatabase.path("primaryKey").asBoolean(false)) {
			constraints.setPrimaryKey(true);
			column.setAutoIncrement(true);
		}
		if (!xDatabase.path("nullable").asBoolean(true)) {
			constraints.setNullable(false);
		}
		if (xDatabase.path("unique").asBoolean(false)) {
			constraints.setUnique(true);
		}
		column.setConstraints(constraints);

		String defaultValue = xDatabase.path("default").asString(null);
		if (defaultValue != null && !defaultValue.isEmpty()) {
			column.setDefaultValue(defaultValue);
		}

		return column;
	}

	private String buildType(JsonNode xDatabase) {
		String type = xDatabase.path("type").asString("varchar");
		int length = xDatabase.path("length").asInt(0);
		int scale = xDatabase.path("scale").asInt(0);

		return this.dataTypeManager.resolveSqlType(type, length, scale);
	}

}
