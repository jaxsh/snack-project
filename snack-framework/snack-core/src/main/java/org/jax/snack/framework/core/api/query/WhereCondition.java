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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * WHERE 条件.
 * <p>
 * 使用 GraphQL 风格的操作符, 如 _eq, _gt, _like, _in 等.
 * <p>
 * 示例: { "age": { "_gt": 18 }, "name": { "_like": "%John%" } }
 *
 * @author Jax Jiang
 */
@Getter
public class WhereCondition {

	/**
	 * 查询条件.
	 * <p>
	 * 使用 GraphQL 风格的操作符, 如 _eq, _gt, _like, _in 等.
	 * <p>
	 * 示例: { "age": { "_gt": 18 }, "name": { "_like": "%John%" } }
	 */
	@Setter(AccessLevel.PROTECTED)
	protected Map<String, Object> where;

	/**
	 * 原子自增更新字段.
	 * <p>
	 * 键为字段名, 值为增量 (正数).
	 * <p>
	 * 示例: { "refCount": 1 }
	 */
	@Setter(AccessLevel.PROTECTED)
	protected Map<String, Number> incrBy;

	/**
	 * 原子自减更新字段.
	 * <p>
	 * 键为字段名, 值为减量 (正数).
	 * <p>
	 * 示例: { "refCount": 1 }
	 */
	@Setter(AccessLevel.PROTECTED)
	protected Map<String, Number> decrBy;

	/**
	 * 追加到 SQL 末尾的自定义片段.
	 * <p>
	 * 示例: "FOR UPDATE", "LIMIT 1"
	 */
	@Setter(AccessLevel.PROTECTED)
	protected String last;

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
	public Builder toBuilder() {
		Builder builder = new Builder();
		if (this.where != null) {
			builder.conditions.putAll(this.where);
		}
		builder.incrByMap = this.incrBy;
		builder.decrByMap = this.decrBy;
		builder.lastSql = this.last;
		return builder;
	}

	/**
	 * WhereCondition 流式构建器.
	 *
	 * @author Jax Jiang
	 */
	public static class Builder {

		protected final Map<String, Object> conditions = new HashMap<>();

		protected Map<String, Number> incrByMap;

		protected Map<String, Number> decrByMap;

		protected String lastSql;

		/**
		 * 设置查询条件 Map.
		 * @param where 条件 Map
		 * @return Builder
		 */
		public Builder where(Map<String, Object> where) {
			if (where != null) {
				this.conditions.putAll(where);
			}
			return this;
		}

		/**
		 * 等于条件 (=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder eq(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.EQ.getValue(), value));
			return this;
		}

		/**
		 * 条件性等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder eqIf(boolean condition, String field, Object value) {
			if (condition) {
				eq(field, value);
			}
			return this;
		}

		/**
		 * 不等于条件 (!=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder ne(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NE.getValue(), value));
			return this;
		}

		/**
		 * 条件性不等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder neIf(boolean condition, String field, Object value) {
			if (condition) {
				ne(field, value);
			}
			return this;
		}

		/**
		 * 大于条件 (>).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder gt(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.GT.getValue(), value));
			return this;
		}

		/**
		 * 条件性大于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder gtIf(boolean condition, String field, Object value) {
			if (condition) {
				gt(field, value);
			}
			return this;
		}

		/**
		 * 大于等于条件 (>=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder gte(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.GTE.getValue(), value));
			return this;
		}

		/**
		 * 条件性大于等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder gteIf(boolean condition, String field, Object value) {
			if (condition) {
				gte(field, value);
			}
			return this;
		}

		/**
		 * 小于条件 (<).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder lt(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LT.getValue(), value));
			return this;
		}

		/**
		 * 条件性小于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder ltIf(boolean condition, String field, Object value) {
			if (condition) {
				lt(field, value);
			}
			return this;
		}

		/**
		 * 小于等于条件 (<=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder lte(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LTE.getValue(), value));
			return this;
		}

		/**
		 * 条件性小于等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder lteIf(boolean condition, String field, Object value) {
			if (condition) {
				lte(field, value);
			}
			return this;
		}

		/**
		 * 模糊匹配 (LIKE '%value%').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder like(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE.getValue(), value));
			return this;
		}

		/**
		 * 条件性模糊匹配.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder likeIf(boolean condition, String field, Object value) {
			if (condition) {
				like(field, value);
			}
			return this;
		}

		/**
		 * 左模糊匹配 (LIKE '%value').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder likeLeft(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE_LEFT.getValue(), value));
			return this;
		}

		/**
		 * 右模糊匹配 (LIKE 'value%').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder likeRight(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE_RIGHT.getValue(), value));
			return this;
		}

		/**
		 * NOT LIKE 条件.
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public Builder notLike(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NOT_LIKE.getValue(), value));
			return this;
		}

		/**
		 * IN 条件.
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public Builder in(String field, Collection<?> values) {
			this.conditions.put(field, Map.of(QueryOperator.IN.getValue(), values));
			return this;
		}

		/**
		 * 条件性 IN.
		 * @param condition 条件
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public Builder inIf(boolean condition, String field, Collection<?> values) {
			if (condition) {
				in(field, values);
			}
			return this;
		}

		/**
		 * NOT IN 条件.
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public Builder nin(String field, Collection<?> values) {
			this.conditions.put(field, Map.of(QueryOperator.NIN.getValue(), values));
			return this;
		}

		/**
		 * IS NULL 条件.
		 * @param field 字段名
		 * @return Builder
		 */
		public Builder isNull(String field) {
			this.conditions.put(field, Map.of(QueryOperator.IS_NULL.getValue(), true));
			return this;
		}

		/**
		 * IS NOT NULL 条件.
		 * @param field 字段名
		 * @return Builder
		 */
		public Builder isNotNull(String field) {
			this.conditions.put(field, Map.of(QueryOperator.IS_NOT_NULL.getValue(), true));
			return this;
		}

		/**
		 * BETWEEN 条件.
		 * @param field 字段名
		 * @param start 起始值
		 * @param end 结束值
		 * @return Builder
		 */
		public Builder between(String field, Object start, Object end) {
			this.conditions.put(field, Map.of(QueryOperator.BETWEEN.getValue(), new Object[] { start, end }));
			return this;
		}

		/**
		 * 原子自增字段.
		 * @param field 字段名
		 * @param delta 增量
		 * @return Builder
		 */
		public Builder incrBy(String field, Number delta) {
			if (this.incrByMap == null) {
				this.incrByMap = new HashMap<>();
			}
			this.incrByMap.put(field, delta);
			return this;
		}

		/**
		 * 原子自减字段.
		 * @param field 字段名
		 * @param delta 减量
		 * @return Builder
		 */
		public Builder decrBy(String field, Number delta) {
			if (this.decrByMap == null) {
				this.decrByMap = new HashMap<>();
			}
			this.decrByMap.put(field, delta);
			return this;
		}

		/**
		 * 追加 SQL 片段.
		 * @param sql SQL 片段
		 * @return Builder
		 */
		public Builder last(String sql) {
			this.lastSql = sql;
			return this;
		}

		/**
		 * 构建 WhereCondition 对象.
		 * @return WhereCondition
		 */
		public WhereCondition build() {
			WhereCondition condition = new WhereCondition();
			condition.setWhere(new HashMap<>(this.conditions));
			condition.setIncrBy(this.incrByMap);
			condition.setDecrBy(this.decrByMap);
			condition.setLast(this.lastSql);
			return condition;
		}

	}

}
