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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
	public static AbstractBuilder<?> builder() {
		return new Builder();
	}

	/**
	 * 将当前条件转换为 Builder 以进行修改.
	 * @return Builder 实例
	 */
	public AbstractBuilder<?> toBuilder() {
		Builder builder = new Builder();
		if (this.where != null) {
			builder.conditions.putAll(this.where);
		}
		builder.lastSql = this.last;
		return builder;
	}

	/**
	 * WhereCondition 流式构建器抽象基类.
	 *
	 * @param <B> 具体 Builder 类型
	 * @author Jax Jiang
	 */
	public abstract static class AbstractBuilder<B extends AbstractBuilder<B>> {

		protected final Map<String, Object> conditions = new HashMap<>();

		protected String lastSql;

		private List<Map<String, Object>> orConditions;

		private List<Map<String, Object>> andConditions;

		/**
		 * 返回具体 Builder 自身.
		 * @return 具体 Builder
		 */
		protected abstract B self();

		/**
		 * 设置查询条件 Map.
		 * @param where 条件 Map
		 * @return Builder
		 */
		public B where(Map<String, Object> where) {
			if (where != null) {
				this.conditions.putAll(where);
			}
			return self();
		}

		/**
		 * 从已有 WhereCondition 整体导入 WHERE 与 last 片段.
		 * @param condition WHERE 条件
		 * @return Builder
		 */
		public B where(WhereCondition condition) {
			if (condition != null) {
				if (condition.getWhere() != null) {
					this.conditions.putAll(condition.getWhere());
				}
				if (condition.getLast() != null) {
					this.lastSql = condition.getLast();
				}
			}
			return self();
		}

		/**
		 * 等于条件 (=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B eq(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.EQ.getValue(), value));
			return self();
		}

		/**
		 * 条件性等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B eqIf(boolean condition, String field, Object value) {
			if (condition) {
				eq(field, value);
			}
			return self();
		}

		/**
		 * 不等于条件 (!=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B ne(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NE.getValue(), value));
			return self();
		}

		/**
		 * 条件性不等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B neIf(boolean condition, String field, Object value) {
			if (condition) {
				ne(field, value);
			}
			return self();
		}

		/**
		 * 大于条件 (>).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B gt(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.GT.getValue(), value));
			return self();
		}

		/**
		 * 条件性大于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B gtIf(boolean condition, String field, Object value) {
			if (condition) {
				gt(field, value);
			}
			return self();
		}

		/**
		 * 大于等于条件 (>=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B gte(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.GTE.getValue(), value));
			return self();
		}

		/**
		 * 条件性大于等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B gteIf(boolean condition, String field, Object value) {
			if (condition) {
				gte(field, value);
			}
			return self();
		}

		/**
		 * 小于条件 (<).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B lt(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LT.getValue(), value));
			return self();
		}

		/**
		 * 条件性小于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B ltIf(boolean condition, String field, Object value) {
			if (condition) {
				lt(field, value);
			}
			return self();
		}

		/**
		 * 小于等于条件 (<=).
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B lte(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LTE.getValue(), value));
			return self();
		}

		/**
		 * 条件性小于等于.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B lteIf(boolean condition, String field, Object value) {
			if (condition) {
				lte(field, value);
			}
			return self();
		}

		/**
		 * 模糊匹配 (LIKE '%value%').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B like(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE.getValue(), value));
			return self();
		}

		/**
		 * 条件性模糊匹配.
		 * @param condition 条件
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B likeIf(boolean condition, String field, Object value) {
			if (condition) {
				like(field, value);
			}
			return self();
		}

		/**
		 * 左模糊匹配 (LIKE '%value').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B likeLeft(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE_LEFT.getValue(), value));
			return self();
		}

		/**
		 * 右模糊匹配 (LIKE 'value%').
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B likeRight(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.LIKE_RIGHT.getValue(), value));
			return self();
		}

		/**
		 * NOT LIKE 条件.
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B notLike(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NOT_LIKE.getValue(), value));
			return self();
		}

		/**
		 * IN 条件.
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public B in(String field, Collection<?> values) {
			this.conditions.put(field, Map.of(QueryOperator.IN.getValue(), values));
			return self();
		}

		/**
		 * 条件性 IN.
		 * @param condition 条件
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public B inIf(boolean condition, String field, Collection<?> values) {
			if (condition) {
				in(field, values);
			}
			return self();
		}

		/**
		 * NOT IN 条件.
		 * @param field 字段名
		 * @param values 值集合
		 * @return Builder
		 */
		public B nin(String field, Collection<?> values) {
			this.conditions.put(field, Map.of(QueryOperator.NIN.getValue(), values));
			return self();
		}

		/**
		 * IS NULL 条件.
		 * @param field 字段名
		 * @return Builder
		 */
		public B isNull(String field) {
			this.conditions.put(field, Map.of(QueryOperator.IS_NULL.getValue(), true));
			return self();
		}

		/**
		 * IS NOT NULL 条件.
		 * @param field 字段名
		 * @return Builder
		 */
		public B isNotNull(String field) {
			this.conditions.put(field, Map.of(QueryOperator.IS_NOT_NULL.getValue(), true));
			return self();
		}

		/**
		 * BETWEEN 条件.
		 * @param field 字段名
		 * @param start 起始值
		 * @param end 结束值
		 * @return Builder
		 */
		public B between(String field, Object start, Object end) {
			this.conditions.put(field, Map.of(QueryOperator.BETWEEN.getValue(), List.of(start, end)));
			return self();
		}

		/**
		 * NOT BETWEEN 条件.
		 * @param field 字段名
		 * @param start 起始值
		 * @param end 结束值
		 * @return Builder
		 */
		public B notBetween(String field, Object start, Object end) {
			this.conditions.put(field, Map.of(QueryOperator.NOT_BETWEEN.getValue(), List.of(start, end)));
			return self();
		}

		/**
		 * NOT LIKE '%value' 条件.
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B notLikeLeft(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NOT_LIKE_LEFT.getValue(), value));
			return self();
		}

		/**
		 * NOT LIKE 'value%' 条件.
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B notLikeRight(String field, Object value) {
			this.conditions.put(field, Map.of(QueryOperator.NOT_LIKE_RIGHT.getValue(), value));
			return self();
		}

		/**
		 * OR 子条件分组.
		 * @param condition 子条件
		 * @return Builder
		 */
		public B or(WhereCondition condition) {
			if (this.orConditions == null) {
				this.orConditions = new ArrayList<>();
			}
			if (condition != null && condition.getWhere() != null) {
				this.orConditions.add(condition.getWhere());
			}
			return self();
		}

		/**
		 * OR 等于条件.
		 * @param field 字段名
		 * @param value 值
		 * @return Builder
		 */
		public B or(String field, Object value) {
			return or(WhereCondition.builder().eq(field, value).build());
		}

		/**
		 * AND 子条件分组.
		 * @param condition 子条件
		 * @return Builder
		 */
		public B and(WhereCondition condition) {
			if (this.andConditions == null) {
				this.andConditions = new ArrayList<>();
			}
			if (condition != null && condition.getWhere() != null) {
				this.andConditions.add(condition.getWhere());
			}
			return self();
		}

		/**
		 * NOT 取反条件.
		 * @param condition 子条件
		 * @return Builder
		 */
		public B not(WhereCondition condition) {
			if (condition != null && condition.getWhere() != null) {
				this.conditions.put(QueryOperator.NOT.getValue(), condition.getWhere());
			}
			return self();
		}

		/**
		 * 追加 SQL 片段.
		 * @param sql SQL 片段
		 * @return Builder
		 */
		public B last(String sql) {
			this.lastSql = sql;
			return self();
		}

		/**
		 * 将基类公共字段 (where/last) 填充到目标条件对象 (含子类).
		 * @param condition 目标条件
		 * @param <C> 条件类型
		 * @return 已填充的条件
		 */
		protected <C extends WhereCondition> C fill(C condition) {
			Map<String, Object> conditionMap = new HashMap<>(this.conditions);
			if (this.orConditions != null) {
				conditionMap.put(QueryOperator.OR.getValue(), this.orConditions);
			}
			if (this.andConditions != null) {
				conditionMap.put(QueryOperator.AND.getValue(), this.andConditions);
			}
			condition.setWhere(conditionMap);
			condition.setLast(this.lastSql);
			return condition;
		}

		/**
		 * 构建条件对象 (子类协变返回各自类型).
		 * @return 条件对象
		 */
		public abstract WhereCondition build();

	}

	/**
	 * WhereCondition 流式构建器.
	 *
	 * @author Jax Jiang
	 */
	public static class Builder extends AbstractBuilder<Builder> {

		@Override
		protected Builder self() {
			return this;
		}

		/**
		 * 构建 WhereCondition 对象.
		 * @return WhereCondition
		 */
		@Override
		public WhereCondition build() {
			return fill(new WhereCondition());
		}

	}

}
