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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
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
public class QueryCondition extends WhereCondition {

	@Setter(AccessLevel.PROTECTED)
	private List<String> select;

	/**
	 * 排序条件列表.
	 * <p>
	 * 支持多字段排序.
	 * <p>
	 * 示例: [{ "field": "createTime", "direction": "desc" }, { "field": "id", "direction":
	 * "asc" }]
	 */
	@Setter(AccessLevel.PROTECTED)
	private List<OrderByCondition> orderBy;

	/**
	 * 每页大小.
	 * <p>
	 * 对应分页查询的页大小, 也对应 SQL 的 LIMIT.
	 */
	@Setter(AccessLevel.PROTECTED)
	private Integer size;

	/**
	 * 当前页码.
	 * <p>
	 * 页码从 1 开始, 用于计算 OFFSET.
	 */
	@Setter(AccessLevel.PROTECTED)
	private Integer current;

	/**
	 * GROUP BY 字段列表.
	 * <p>
	 * 字段名使用 camelCase, 自动映射为数据库 snake_case 列名.
	 */
	@Setter(AccessLevel.PROTECTED)
	private List<String> groupBy;

	/**
	 * HAVING 子句.
	 * <p>
	 * 原始 SQL 片段, 如 "COUNT(*) > 5".
	 */
	@Setter(AccessLevel.PROTECTED)
	private String having;

	/**
	 * 创建 Builder 实例.
	 * @return 新的 Builder 实例
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 将当前条件转换为 Builder 以进行修改.
	 * @return Builder 实例
	 */
	@Override
	public Builder toBuilder() {
		Builder builder = new Builder();
		if (this.where != null) {
			builder.conditions.putAll(this.where);
		}
		builder.incrByMap = this.incrBy;
		builder.decrByMap = this.decrBy;
		builder.lastSql = this.last;
		builder.selectFields = this.select;
		builder.orderByList = this.orderBy;
		builder.pageSize = this.size;
		builder.currentPage = this.current;
		builder.groupByList = this.groupBy;
		builder.havingSql = this.having;
		return builder;
	}

	/**
	 * QueryCondition 流式构建器.
	 *
	 * @author Jax Jiang
	 */
	public static class Builder extends WhereCondition.Builder {

		private List<String> selectFields;

		private List<OrderByCondition> orderByList;

		private Integer pageSize;

		private Integer currentPage;

		private List<String> groupByList;

		private String havingSql;

		@Override
		public Builder eq(String field, Object value) {
			super.eq(field, value);
			return this;
		}

		@Override
		public Builder eqIf(boolean condition, String field, Object value) {
			super.eqIf(condition, field, value);
			return this;
		}

		@Override
		public Builder ne(String field, Object value) {
			super.ne(field, value);
			return this;
		}

		@Override
		public Builder neIf(boolean condition, String field, Object value) {
			super.neIf(condition, field, value);
			return this;
		}

		@Override
		public Builder gt(String field, Object value) {
			super.gt(field, value);
			return this;
		}

		@Override
		public Builder gtIf(boolean condition, String field, Object value) {
			super.gtIf(condition, field, value);
			return this;
		}

		@Override
		public Builder gte(String field, Object value) {
			super.gte(field, value);
			return this;
		}

		@Override
		public Builder gteIf(boolean condition, String field, Object value) {
			super.gteIf(condition, field, value);
			return this;
		}

		@Override
		public Builder lt(String field, Object value) {
			super.lt(field, value);
			return this;
		}

		@Override
		public Builder ltIf(boolean condition, String field, Object value) {
			super.ltIf(condition, field, value);
			return this;
		}

		@Override
		public Builder lte(String field, Object value) {
			super.lte(field, value);
			return this;
		}

		@Override
		public Builder lteIf(boolean condition, String field, Object value) {
			super.lteIf(condition, field, value);
			return this;
		}

		@Override
		public Builder like(String field, Object value) {
			super.like(field, value);
			return this;
		}

		@Override
		public Builder likeIf(boolean condition, String field, Object value) {
			super.likeIf(condition, field, value);
			return this;
		}

		@Override
		public Builder likeLeft(String field, Object value) {
			super.likeLeft(field, value);
			return this;
		}

		@Override
		public Builder likeRight(String field, Object value) {
			super.likeRight(field, value);
			return this;
		}

		@Override
		public Builder notLike(String field, Object value) {
			super.notLike(field, value);
			return this;
		}

		@Override
		public Builder in(String field, Collection<?> values) {
			super.in(field, values);
			return this;
		}

		@Override
		public Builder inIf(boolean condition, String field, Collection<?> values) {
			super.inIf(condition, field, values);
			return this;
		}

		@Override
		public Builder nin(String field, Collection<?> values) {
			super.nin(field, values);
			return this;
		}

		@Override
		public Builder isNull(String field) {
			super.isNull(field);
			return this;
		}

		@Override
		public Builder isNotNull(String field) {
			super.isNotNull(field);
			return this;
		}

		@Override
		public Builder between(String field, Object start, Object end) {
			super.between(field, start, end);
			return this;
		}

		@Override
		public Builder notBetween(String field, Object start, Object end) {
			super.notBetween(field, start, end);
			return this;
		}

		@Override
		public Builder notLikeLeft(String field, Object value) {
			super.notLikeLeft(field, value);
			return this;
		}

		@Override
		public Builder notLikeRight(String field, Object value) {
			super.notLikeRight(field, value);
			return this;
		}

		@Override
		public Builder incrBy(String field, Number delta) {
			super.incrBy(field, delta);
			return this;
		}

		@Override
		public Builder decrBy(String field, Number delta) {
			super.decrBy(field, delta);
			return this;
		}

		@Override
		public Builder last(String sql) {
			super.last(sql);
			return this;
		}

		@Override
		public Builder and(WhereCondition condition) {
			super.and(condition);
			return this;
		}

		@Override
		public Builder or(WhereCondition condition) {
			super.or(condition);
			return this;
		}

		@Override
		public Builder not(WhereCondition condition) {
			super.not(condition);
			return this;
		}

		@Override
		public Builder or(String field, Object value) {
			super.or(field, value);
			return this;
		}

		@Override
		public Builder where(Map<String, Object> where) {
			super.where(where);
			return this;
		}

		/**
		 * 设置查询字段.
		 * @param fields 字段名数组
		 * @return Builder
		 */
		public Builder select(String... fields) {
			this.selectFields = Arrays.asList(fields);
			return this;
		}

		/**
		 * 设置分页.
		 * @param current 当前页码 (从 1 开始)
		 * @param size 每页大小
		 * @return Builder
		 */
		public Builder page(int current, int size) {
			this.currentPage = current;
			this.pageSize = size;
			return this;
		}

		/**
		 * 设置每页大小.
		 * @param size 每页大小
		 * @return Builder
		 */
		public Builder size(int size) {
			this.pageSize = size;
			return this;
		}

		/**
		 * 设置当前页码.
		 * @param current 当前页码 (从 1 开始)
		 * @return Builder
		 */
		public Builder current(int current) {
			this.currentPage = current;
			return this;
		}

		/**
		 * 升序排序.
		 * @param field 排序字段
		 * @return Builder
		 */
		public Builder orderByAsc(String field) {
			addOrderBy(field, "asc");
			return this;
		}

		/**
		 * 降序排序.
		 * @param field 排序字段
		 * @return Builder
		 */
		public Builder orderByDesc(String field) {
			addOrderBy(field, "desc");
			return this;
		}

		/**
		 * 设置排序列表.
		 * @param orderByList 排序列表
		 * @return Builder
		 */
		public Builder orderBy(List<OrderByCondition> orderByList) {
			this.orderByList = orderByList;
			return this;
		}

		/**
		 * GROUP BY 字段.
		 * @param fields 字段名 (camelCase)
		 * @return Builder
		 */
		public Builder groupBy(String... fields) {
			this.groupByList = Arrays.asList(fields);
			return this;
		}

		/**
		 * HAVING 子句.
		 * @param sql SQL 片段, 如 "COUNT(*) > 5"
		 * @return Builder
		 */
		public Builder having(String sql) {
			this.havingSql = sql;
			return this;
		}

		private void addOrderBy(String field, String direction) {
			if (this.orderByList == null) {
				this.orderByList = new ArrayList<>();
			}
			OrderByCondition order = new OrderByCondition();
			order.setField(field);
			order.setDirection(direction);
			this.orderByList.add(order);
		}

		/**
		 * 构建 QueryCondition 对象.
		 * @return QueryCondition
		 */
		@Override
		public QueryCondition build() {
			QueryCondition condition = new QueryCondition();
			condition.where = new HashMap<>(this.conditions);
			condition.incrBy = this.incrByMap;
			condition.decrBy = this.decrByMap;
			condition.last = this.lastSql;
			condition.select = this.selectFields;
			condition.orderBy = this.orderByList;
			condition.size = this.pageSize;
			condition.current = this.currentPage;
			condition.groupBy = this.groupByList;
			condition.having = this.havingSql;
			return condition;
		}

	}

}
