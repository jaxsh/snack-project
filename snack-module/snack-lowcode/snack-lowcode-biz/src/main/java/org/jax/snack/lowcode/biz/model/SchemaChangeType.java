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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.BaseEnum;

/**
 * Schema 变更类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum SchemaChangeType implements BaseEnum<String> {

	/**
	 * 创建表.
	 */
	ADD_TABLE("ADD_TABLE", "创建表"),

	/**
	 * 新增列.
	 */
	ADD_COLUMN("ADD_COLUMN", "新增列"),

	/**
	 * 删除列.
	 */
	DROP_COLUMN("DROP_COLUMN", "删除列"),

	/**
	 * 修改列类型.
	 */
	MODIFY_COLUMN_TYPE("MODIFY_COLUMN_TYPE", "修改列类型"),

	/**
	 * 修改列长度.
	 */
	MODIFY_LENGTH("MODIFY_LENGTH", "修改列长度"),

	/**
	 * 修改可空约束.
	 */
	MODIFY_NULLABLE("MODIFY_NULLABLE", "修改可空约束"),

	/**
	 * 修改默认值.
	 */
	MODIFY_DEFAULT("MODIFY_DEFAULT", "修改默认值"),

	/**
	 * 新增索引.
	 */
	ADD_INDEX("ADD_INDEX", "新增索引"),

	/**
	 * 删除索引.
	 */
	DROP_INDEX("DROP_INDEX", "删除索引"),

	/**
	 * 重命名列.
	 */
	RENAME_COLUMN("RENAME_COLUMN", "重命名列");

	/**
	 * 状态值.
	 */
	private final String code;

	/**
	 * 状态名称.
	 */
	private final String name;

	/**
	 * 根据 code 获取枚举实例.
	 * @param code 状态值
	 * @return SchemaChangeType 实例
	 */
	public static SchemaChangeType of(String code) {
		return BaseEnum.fromCode(SchemaChangeType.class, code);
	}

}
