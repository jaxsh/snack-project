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

package org.jax.snack.framework.core.api.query;

import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 通用查询条件.
 * <p>
 * 支持字段选择、条件过滤、排序等功能, 语法参考 GraphQL/Hasura.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class QueryCondition {

	/**
	 * 字段选择列表.
	 * <p>
	 * 指定要查询的字段. 如果为 null 或空, 则查询所有字段.
	 * <p>
	 * 示例: ["id", "name", "email"]
	 */
	private List<String> select;

	/**
	 * 查询条件.
	 * <p>
	 * 使用 GraphQL 风格的操作符, 如 _eq, _gt, _like, _in 等.
	 * <p>
	 * 示例: { "age": { "_gt": 18 }, "name": { "_like": "%John%" } }
	 */
	private Map<String, Object> where;

	/**
	 * 排序条件列表.
	 * <p>
	 * 支持多字段排序.
	 * <p>
	 * 示例: [{ "field": "createTime", "direction": "desc" }, { "field": "id", "direction":
	 * "asc" }]
	 */
	private List<OrderByCondition> orderBy;

	/**
	 * 每页大小.
	 * <p>
	 * 对应分页查询的页大小, 也对应 SQL 的 LIMIT.
	 */
	private Integer size;

	/**
	 * 当前页码.
	 * <p>
	 * 页码从 1 开始, 用于计算 OFFSET.
	 */
	private Integer current;

}
