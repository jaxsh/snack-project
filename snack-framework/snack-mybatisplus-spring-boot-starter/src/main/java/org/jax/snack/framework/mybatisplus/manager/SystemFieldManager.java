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

package org.jax.snack.framework.mybatisplus.manager;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.mybatisplus.context.UserContextProvider;

import org.springframework.stereotype.Component;

/**
 * 系统字段管理器.
 * <p>
 * 统一管理系统级强制字段（主键、审计字段）的元数据定义和运行时值填充. 替代原有的 AuditManager，提供更全面的 Schema 描述能力.
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class SystemFieldManager {

	/**
	 * 字段名: ID.
	 */
	public static final String FIELD_ID = "id";

	/**
	 * 列名: ID.
	 */
	public static final String COLUMN_ID = "id";

	/**
	 * 字段名: 创建时间.
	 */
	public static final String FIELD_CREATE_TIME = "createTime";

	/**
	 * 列名: 创建时间.
	 */
	public static final String COLUMN_CREATE_TIME = "create_time";

	/**
	 * 字段名: 创建人.
	 */
	public static final String FIELD_CREATE_BY = "createBy";

	/**
	 * 列名: 创建人.
	 */
	public static final String COLUMN_CREATE_BY = "create_by";

	/**
	 * 字段名: 更新时间.
	 */
	public static final String FIELD_UPDATE_TIME = "updateTime";

	/**
	 * 列名: 更新时间.
	 */
	public static final String COLUMN_UPDATE_TIME = "update_time";

	/**
	 * 字段名: 更新人.
	 */
	public static final String FIELD_UPDATE_BY = "updateBy";

	/**
	 * 列名: 更新人.
	 */
	public static final String COLUMN_UPDATE_BY = "update_by";

	private static final String TYPE_TIMESTAMP = "TIMESTAMP";

	private static final String TYPE_VARCHAR = "varchar";

	private static final int LENGTH_VARCHAR = 64;

	private static final String TYPE_BIGINT = "bigint";

	private static final String LOGIC_TYPE_LONG = "long";

	private static final String LOGIC_TYPE_TIMESTAMP = "timestamp";

	private static final String LOGIC_TYPE_STRING = "string";

	private static final String DEFAULT_CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";

	private static final List<SystemField> SYSTEM_FIELDS;

	static {

		SYSTEM_FIELDS = List.of(
				new SystemField(COLUMN_ID, FIELD_ID, LOGIC_TYPE_LONG, TYPE_BIGINT, null, "ID", null, true, true, true),
				new SystemField(COLUMN_CREATE_TIME, FIELD_CREATE_TIME, LOGIC_TYPE_TIMESTAMP, TYPE_TIMESTAMP, null,
						"创建时间", DEFAULT_CURRENT_TIMESTAMP, false, false, true),
				new SystemField(COLUMN_CREATE_BY, FIELD_CREATE_BY, LOGIC_TYPE_STRING, TYPE_VARCHAR, LENGTH_VARCHAR,
						"创建人", null, false, false, true),
				new SystemField(COLUMN_UPDATE_TIME, FIELD_UPDATE_TIME, LOGIC_TYPE_TIMESTAMP, TYPE_TIMESTAMP, null,
						"更新时间", DEFAULT_CURRENT_TIMESTAMP, false, false, true),
				new SystemField(COLUMN_UPDATE_BY, FIELD_UPDATE_BY, LOGIC_TYPE_STRING, TYPE_VARCHAR, LENGTH_VARCHAR,
						"更新人", null, false, false, true));
	}

	private final UserContextProvider userContextProvider;

	/**
	 * 获取所有系统字段定义.
	 * @return 不可变列表
	 */
	public List<SystemField> getSystemFields() {
		return SYSTEM_FIELDS;
	}

	/**
	 * 填充系统字段参数 (Map 模式).
	 * <p>
	 * 主要用于填充审计字段，ID 通常由数据库自增生成，不需要在此填充.
	 * @param params 参数 Map
	 * @param isUpdate 是否为更新操作
	 */
	public void fillSystemParams(Map<String, Object> params, boolean isUpdate) {
		String user = this.userContextProvider.getCurrentUsername();
		ZonedDateTime now = ZonedDateTime.now();

		params.put(COLUMN_UPDATE_BY, user);
		params.put(COLUMN_UPDATE_TIME, now);

		if (!isUpdate) {
			params.putIfAbsent(COLUMN_CREATE_BY, user);
			params.putIfAbsent(COLUMN_CREATE_TIME, now);
		}
	}

	/**
	 * 系统字段定义.
	 *
	 * @param columnName 数据库列名 (snake_case)
	 * @param fieldName JSON 字段名 (camelCase)
	 * @param logicType 逻辑类型 (用于 Lowcode 映射)
	 * @param sqlType SQL 类型
	 * @param length 字段长度 (可为 null)
	 * @param remarks 注释
	 * @param defaultValueComputed 默认值表达式
	 * @param isPrimaryKey 是否主键
	 * @param isAutoIncrement 是否自增
	 * @param isSystem 是否系统字段 (用于标记不可变)
	 */
	public record SystemField(String columnName, String fieldName, String logicType, String sqlType, Integer length,
			String remarks, String defaultValueComputed, boolean isPrimaryKey, boolean isAutoIncrement,
			boolean isSystem) {
	}

}
