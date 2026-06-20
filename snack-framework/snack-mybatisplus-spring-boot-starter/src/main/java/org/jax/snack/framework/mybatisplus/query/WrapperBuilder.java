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

package org.jax.snack.framework.mybatisplus.query;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.OrderByCondition;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.query.UpdateCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 统一 Wrapper 构建器.
 * <p>
 * 将 DSL 条件转换为 MyBatis-Plus 的 Wrapper, 支持 Query/Update/Delete.
 *
 * @author Jax Jiang
 */
@Slf4j
public final class WrapperBuilder {

	private WrapperBuilder() {
	}

	/**
	 * 构建 QueryWrapper (基于 Entity).
	 * @param condition 查询条件
	 * @param entityClass 实体类
	 * @param <T> 实体类型
	 * @return QueryWrapper
	 */
	public static <T> QueryWrapper<T> query(QueryCondition condition, Class<T> entityClass) {
		QueryWrapper<T> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();
		if (condition == null) {
			return wrapper;
		}
		Set<String> validFields = getValidFields(entityClass);

		if (!CollectionUtils.isEmpty(condition.getSelect())) {
			applySelect(wrapper, condition.getSelect(), validFields, entityClass);
		}

		Function<String, String> columnResolver = createColumnResolver(entityClass, validFields);
		applyConditions(wrapper, condition, columnResolver);
		applyLast(wrapper, condition);
		return wrapper;
	}

	/**
	 * 构建 QueryWrapper (基于 Map 映射).
	 * @param condition 查询条件
	 * @param fieldMapping 字段名到数据库列名的映射
	 * @return QueryWrapper
	 */
	public static QueryWrapper<Void> query(QueryCondition condition, Map<String, String> fieldMapping) {
		QueryWrapper<Void> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();
		if (condition == null) {
			return wrapper;
		}

		Function<String, String> columnResolver = createColumnResolver(fieldMapping);
		applyConditions(wrapper, condition, columnResolver);
		applyLast(wrapper, condition);
		return wrapper;
	}

	/**
	 * 构建 WHERE 条件 (基于 Entity).
	 * @param condition WHERE 条件
	 * @param entityClass 实体类
	 * @param <T> 实体类型
	 * @return QueryWrapper
	 */
	public static <T> QueryWrapper<T> where(WhereCondition condition, Class<T> entityClass) {
		QueryWrapper<T> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();
		if (condition == null) {
			return wrapper;
		}
		Set<String> validFields = getValidFields(entityClass);
		Function<String, String> columnResolver = createColumnResolver(entityClass, validFields);

		if (!ObjectUtils.isEmpty(condition.getWhere())) {
			applyWhere(wrapper, condition.getWhere(), columnResolver);
		}

		applyLast(wrapper, condition);
		return wrapper;
	}

	/**
	 * 构建 WHERE 条件 (基于 Map 映射).
	 * @param condition WHERE 条件
	 * @param fieldMapping 字段名到数据库列名的映射
	 * @return QueryWrapper
	 */
	public static QueryWrapper<Void> where(WhereCondition condition, Map<String, String> fieldMapping) {
		QueryWrapper<Void> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();
		if (condition == null) {
			return wrapper;
		}
		Function<String, String> columnResolver = createColumnResolver(fieldMapping);

		if (!ObjectUtils.isEmpty(condition.getWhere())) {
			applyWhere(wrapper, condition.getWhere(), columnResolver);
		}

		applyLast(wrapper, condition);
		return wrapper;
	}

	/**
	 * 构建 UpdateWrapper (基于 UpdateCondition).
	 * @param condition 更新条件
	 * @param entityClass 实体类
	 * @param <T> 实体类型
	 * @return UpdateWrapper
	 */
	public static <T> UpdateWrapper<T> update(UpdateCondition condition, Class<T> entityClass) {
		UpdateWrapper<T> wrapper = new UpdateWrapper<>();
		wrapper.checkSqlInjection();
		if (condition == null) {
			return wrapper;
		}
		Set<String> validFields = getValidFields(entityClass);
		Function<String, String> columnResolver = createColumnResolver(entityClass, validFields);

		if (!CollectionUtils.isEmpty(condition.getSet())) {
			applySet(wrapper, condition.getSet(), columnResolver);
		}
		if (condition.getNulls() != null) {
			applySet(wrapper, JsonNullableSupport.clearedFields(condition.getNulls()), columnResolver);
		}
		applyIncrBy(wrapper, condition, columnResolver);
		applyDecrBy(wrapper, condition, columnResolver);
		applyWhereForUpdate(wrapper, condition, columnResolver);
		applyLast(wrapper, condition);

		return wrapper;
	}

	/**
	 * 构建 UpdateWrapper (Map 驱动, Lowcode 动态场景).
	 * @param setData 要更新的字段
	 * @param condition WHERE 条件
	 * @param fieldMapping 字段名到数据库列名的映射
	 * @return UpdateWrapper
	 */
	public static UpdateWrapper<Void> update(Map<String, Object> setData, WhereCondition condition,
			Map<String, String> fieldMapping) {
		UpdateWrapper<Void> wrapper = new UpdateWrapper<>();
		wrapper.checkSqlInjection();
		Function<String, String> columnResolver = createColumnResolver(fieldMapping);

		applySet(wrapper, setData, columnResolver);
		applyWhereForUpdate(wrapper, condition, columnResolver);
		applyLast(wrapper, condition);

		return wrapper;
	}

	private static <T> Function<String, String> createColumnResolver(Class<T> entityClass, Set<String> validFields) {
		return (field) -> {
			if (!validFields.contains(field)) {
				log.debug("Field {} does not exist in entity {}, ignored", field, entityClass.getSimpleName());
				return null;
			}
			return getColumnName(entityClass, field);
		};
	}

	private static Function<String, String> createColumnResolver(Map<String, String> fieldMapping) {
		return (field) -> {
			String column = fieldMapping.get(field);
			if (column == null) {
				log.debug("Field {} not found in mapping, ignored", field);
			}
			return column;
		};
	}

	private static <T> void applySet(UpdateWrapper<T> wrapper, Map<String, Object> setData,
			Function<String, String> columnResolver) {
		setData.forEach((field, value) -> {
			String column = columnResolver.apply(field);
			if (column != null) {
				wrapper.set(column, value);
			}
		});
	}

	private static <T> void applyIncrBy(UpdateWrapper<T> wrapper, UpdateCondition condition,
			Function<String, String> columnResolver) {
		if (condition == null || CollectionUtils.isEmpty(condition.getIncrBy())) {
			return;
		}
		condition.getIncrBy().forEach((field, value) -> {
			String column = columnResolver.apply(field);
			if (column != null && value != null) {
				wrapper.setIncrBy(column, value);
			}
		});
	}

	private static <T> void applyDecrBy(UpdateWrapper<T> wrapper, UpdateCondition condition,
			Function<String, String> columnResolver) {
		if (condition == null || CollectionUtils.isEmpty(condition.getDecrBy())) {
			return;
		}
		condition.getDecrBy().forEach((field, value) -> {
			String column = columnResolver.apply(field);
			if (column != null && value != null) {
				wrapper.setDecrBy(column, value);
			}
		});
	}

	private static <T> void applyWhereForUpdate(UpdateWrapper<T> wrapper, WhereCondition condition,
			Function<String, String> columnResolver) {
		if (condition != null && !ObjectUtils.isEmpty(condition.getWhere())) {
			applyWhere(wrapper, condition.getWhere(), columnResolver);
		}
	}

	private static <T> void applySelect(QueryWrapper<T> wrapper, List<String> selectFields, Set<String> validFields,
			Class<T> entityClass) {
		List<String> filteredFields = selectFields.stream().filter(validFields::contains).toList();

		if (filteredFields.isEmpty()) {
			log.warn("All selected fields are invalid, selecting all fields");
			return;
		}

		wrapper.select(entityClass, (field) -> filteredFields.contains(field.getProperty()));
	}

	private static <T> void applyConditions(QueryWrapper<T> wrapper, QueryCondition condition,
			Function<String, String> columnResolver) {
		if (condition == null) {
			return;
		}
		if (!ObjectUtils.isEmpty(condition.getWhere())) {
			applyWhere(wrapper, condition.getWhere(), columnResolver);
		}

		if (!CollectionUtils.isEmpty(condition.getOrderBy())) {
			applyOrderBy(wrapper, condition.getOrderBy(), columnResolver);
		}

		if (!CollectionUtils.isEmpty(condition.getGroupBy())) {
			applyGroupBy(wrapper, condition.getGroupBy(), columnResolver);
		}

		if (StringUtils.hasText(condition.getHaving())) {
			wrapper.having(condition.getHaving());
		}
	}

	private static <T> void applyGroupBy(QueryWrapper<T> wrapper, List<String> groupByFields,
			Function<String, String> columnResolver) {
		groupByFields.stream().map(columnResolver).filter(Objects::nonNull).forEach(wrapper::groupBy);
	}

	private static void applyWhere(AbstractWrapper<?, String, ?> wrapper, Map<?, ?> where,
			Function<String, String> columnResolver) {
		where.forEach((key, value) -> {
			if (!(key instanceof String k)) {
				return;
			}
			QueryOperator logicOp = QueryOperator.fromValue(k);
			if (logicOp == QueryOperator.AND) {
				if (value instanceof List<?> list) {
					if (wrapper instanceof QueryWrapper<?> qw) {
						qw.and((w) -> list.forEach((item) -> {
							if (item instanceof Map<?, ?> map) {
								applyWhere(w, map, columnResolver);
							}
						}));
					}
					else if (wrapper instanceof UpdateWrapper<?> uw) {
						uw.and((w) -> list.forEach((item) -> {
							if (item instanceof Map<?, ?> map) {
								applyWhere(w, map, columnResolver);
							}
						}));
					}
				}
				return;
			}
			if (logicOp == QueryOperator.OR) {
				if (value instanceof List<?> list && !CollectionUtils.isEmpty(list)) {
					applyOrConditions(wrapper, list, columnResolver);
				}
				return;
			}
			if (logicOp == QueryOperator.NOT) {
				if (value instanceof Map<?, ?> map) {
					if (wrapper instanceof QueryWrapper<?> qw) {
						qw.not((w) -> applyWhere(w, map, columnResolver));
					}
					else if (wrapper instanceof UpdateWrapper<?> uw) {
						uw.not((w) -> applyWhere(w, map, columnResolver));
					}
				}
				return;
			}

			String column = columnResolver.apply(k);
			if (column == null) {
				return;
			}

			if (value instanceof Map<?, ?> map) {
				map.forEach((op, val) -> {
					if (op instanceof String opStr) {
						applyOperator(wrapper, column, opStr, val);
					}
				});
			}
			else {
				applyOperator(wrapper, column, QueryOperator.EQ.getValue(), value);
			}
		});
	}

	private static void applyOrConditions(AbstractWrapper<?, String, ?> wrapper, List<?> list,
			Function<String, String> columnResolver) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof Map<?, ?> map) {
				if (i == 0) {
					if (wrapper instanceof QueryWrapper<?> qw) {
						qw.and((w) -> applyWhere(w, map, columnResolver));
					}
					else if (wrapper instanceof UpdateWrapper<?> uw) {
						uw.and((w) -> applyWhere(w, map, columnResolver));
					}
				}
				else {
					if (wrapper instanceof QueryWrapper<?> qw) {
						qw.or((w) -> applyWhere(w, map, columnResolver));
					}
					else if (wrapper instanceof UpdateWrapper<?> uw) {
						uw.or((w) -> applyWhere(w, map, columnResolver));
					}
				}
			}
		}
	}

	private static void applyOperator(AbstractWrapper<?, String, ?> wrapper, String column, String operatorStr,
			Object value) {
		QueryOperator operator = QueryOperator.fromValue(operatorStr);
		if (operator == null) {
			log.warn("Unknown operator: {}, ignored", operatorStr);
			return;
		}

		applyOperatorToWrapper(wrapper, column, operator, value);
	}

	private static void applyOperatorToWrapper(AbstractWrapper<?, String, ?> wrapper, String column,
			QueryOperator operator, Object value) {
		switch (operator) {
			case EQ -> wrapper.eq(column, value);
			case NE -> wrapper.ne(column, value);
			case GT -> wrapper.gt(column, value);
			case GTE -> wrapper.ge(column, value);
			case LT -> wrapper.lt(column, value);
			case LTE -> wrapper.le(column, value);
			case LIKE -> wrapper.like(column, value);
			case LIKE_LEFT -> wrapper.likeLeft(column, value);
			case LIKE_RIGHT -> wrapper.likeRight(column, value);
			case NOT_LIKE -> wrapper.notLike(column, value);
			case IN -> {
				if (value instanceof Collection<?> collection) {
					wrapper.in(column, collection);
				}
			}
			case NIN -> {
				if (value instanceof Collection<?> collection) {
					wrapper.notIn(column, collection);
				}
			}
			case IS_NULL -> wrapper.isNull(column);
			case IS_NOT_NULL -> wrapper.isNotNull(column);
			case BETWEEN -> {
				if (value instanceof List<?> list && list.size() == 2) {
					wrapper.between(column, list.get(0), list.get(1));
				}
			}
			case NOT_BETWEEN -> {
				if (value instanceof List<?> list && list.size() == 2) {
					wrapper.notBetween(column, list.get(0), list.get(1));
				}
			}
			case NOT_LIKE_LEFT -> wrapper.notLikeLeft(column, value);
			case NOT_LIKE_RIGHT -> wrapper.notLikeRight(column, value);
			default -> log.warn("Unsupported operator: {}", operator);
		}
	}

	private static <T> void applyOrderBy(QueryWrapper<T> wrapper, List<OrderByCondition> orderByList,
			Function<String, String> columnResolver) {
		orderByList.forEach((order) -> {
			String column = columnResolver.apply(order.getField());
			if (column != null) {
				boolean asc = !"desc".equalsIgnoreCase(order.getDirection());
				wrapper.orderBy(true, asc, column);
			}
		});
	}

	private static <T> Set<String> getValidFields(Class<T> entityClass) {
		TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
		if (tableInfo == null) {
			log.warn("Cannot get TableInfo for entity class {}", entityClass.getName());
			return Set.of();
		}

		Set<String> fields = tableInfo.getFieldList()
			.stream()
			.map(TableFieldInfo::getProperty)
			.collect(Collectors.toSet());

		if (tableInfo.getKeyProperty() != null) {
			fields.add(tableInfo.getKeyProperty());
		}

		return fields;
	}

	private static String getColumnName(Class<?> entityClass, String fieldName) {
		Map<String, ColumnCache> columnMap = LambdaUtils.getColumnMap(entityClass);
		if (columnMap == null) {
			return null;
		}
		ColumnCache columnCache = columnMap.get(fieldName.toUpperCase(Locale.ENGLISH));
		return (columnCache != null) ? columnCache.getColumn() : null;
	}

	private static void applyLast(AbstractWrapper<?, ?, ?> wrapper, WhereCondition condition) {
		if (condition != null && StringUtils.hasText(condition.getLast())) {
			wrapper.last(condition.getLast());
		}
	}

}
