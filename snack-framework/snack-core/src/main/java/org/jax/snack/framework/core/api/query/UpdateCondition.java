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

import java.util.HashMap;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用更新条件.
 * <p>
 * 在 WHERE 条件基础上增加 SET 置空与原子自增减.
 *
 * @author Jax Jiang
 */
@Getter
public class UpdateCondition extends WhereCondition {

	/**
	 * 显式置空的列 (键为字段名, 值为 null).
	 */
	@Setter(AccessLevel.PROTECTED)
	private Map<String, Object> set;

	/**
	 * 待解包的 DTO, 其显式传入 null 的字段将被更新为 NULL.
	 */
	@Setter(AccessLevel.PROTECTED)
	private Object nulls;

	/**
	 * 原子自增更新字段.
	 * <p>
	 * 键为字段名, 值为增量 (正数). 示例: { "refCount": 1 }
	 */
	@Setter(AccessLevel.PROTECTED)
	private Map<String, Number> incrBy;

	/**
	 * 原子自减更新字段.
	 * <p>
	 * 键为字段名, 值为减量 (正数). 示例: { "refCount": 1 }
	 */
	@Setter(AccessLevel.PROTECTED)
	private Map<String, Number> decrBy;

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
		builder.lastSql = this.last;
		builder.setData = this.set;
		builder.nullsBean = this.nulls;
		builder.incrByMap = this.incrBy;
		builder.decrByMap = this.decrBy;
		return builder;
	}

	/**
	 * UpdateCondition 流式构建器.
	 *
	 * @author Jax Jiang
	 */
	public static class Builder extends WhereCondition.AbstractBuilder<Builder> {

		private Map<String, Object> setData;

		private Object nullsBean;

		private Map<String, Number> incrByMap;

		private Map<String, Number> decrByMap;

		@Override
		protected Builder self() {
			return this;
		}

		/**
		 * 显式将单个字段置为 NULL.
		 * @param field 字段名
		 * @return Builder
		 */
		public Builder setNull(String field) {
			if (this.setData == null) {
				this.setData = new HashMap<>();
			}
			this.setData.put(field, null);
			return this;
		}

		/**
		 * 传入 DTO, 其显式置 null 的字段将被更新为 NULL.
		 * @param dto DTO 对象
		 * @return Builder
		 */
		public Builder setNulls(Object dto) {
			this.nullsBean = dto;
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
		 * 构建 UpdateCondition 对象.
		 * @return UpdateCondition
		 */
		@Override
		public UpdateCondition build() {
			UpdateCondition condition = fill(new UpdateCondition());
			condition.set = this.setData;
			condition.nulls = this.nullsBean;
			condition.incrBy = this.incrByMap;
			condition.decrBy = this.decrByMap;
			return condition;
		}

	}

}
