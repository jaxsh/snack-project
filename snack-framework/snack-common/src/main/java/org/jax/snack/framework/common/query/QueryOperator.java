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

package org.jax.snack.framework.common.query;

import lombok.Getter;

/**
 * 查询操作符枚举.
 * <p>
 * GraphQL/Hasura 风格的操作符.
 *
 * @author Jax Jiang
 */
@Getter
public enum QueryOperator {

	/**
	 * 等于 (=).
	 */
	EQ("_eq"),

	/**
	 * 不等于 (!=).
	 */
	NE("_ne"),

	/**
	 * 大于 (>).
	 */
	GT("_gt"),

	/**
	 * 大于等于 (>=).
	 */
	GTE("_gte"),

	/**
	 * 小于 (<).
	 */
	LT("_lt"),

	/**
	 * 小于等于 (<=).
	 */
	LTE("_lte"),

	/**
	 * 模糊匹配 (LIKE).
	 */
	LIKE("_like"),

	/**
	 * 不区分大小写的模糊匹配 (ILIKE).
	 */
	ILIKE("_ilike"),

	/**
	 * 左模糊匹配 (LIKE '%value').
	 */
	LIKE_LEFT("_like_left"),

	/**
	 * 右模糊匹配 (LIKE 'value%').
	 */
	LIKE_RIGHT("_like_right"),

	/**
	 * 否定模糊匹配 (NOT LIKE).
	 */
	NOT_LIKE("_not_like"),

	/**
	 * 在...中 (IN).
	 */
	IN("_in"),

	/**
	 * 不在...中 (NOT IN).
	 */
	NIN("_nin"),

	/**
	 * 为空 (IS NULL).
	 */
	IS_NULL("_is_null"),

	/**
	 * 不为空 (IS NOT NULL).
	 */
	IS_NOT_NULL("_is_not_null"),

	/**
	 * 逻辑与 (AND).
	 */
	AND("_and"),

	/**
	 * 逻辑或 (OR).
	 */
	OR("_or"),

	/**
	 * 区间查询 (BETWEEN).
	 */
	BETWEEN("_between"),

	/**
	 * 逻辑非 (NOT).
	 */
	NOT("_not");

	private final String value;

	QueryOperator(String value) {
		this.value = value;
	}

	/**
	 * 根据字符串值获取操作符.
	 * @param value 操作符字符串
	 * @return 操作符枚举, 如果不存在则返回 null
	 */
	public static QueryOperator fromValue(String value) {
		for (QueryOperator op : values()) {
			if (op.value.equals(value)) {
				return op;
			}
		}
		return null;
	}

}
