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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.OrderByCondition;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;

import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 查询条件构建器.
 * <p>
 * 将 {@link QueryCondition} 转换为 MyBatis-Plus 的 {@link QueryWrapper}.
 *
 * @author Jax Jiang
 */
@Slf4j
public final class QueryWrapperBuilder {

	private QueryWrapperBuilder() {
	}

	/**
	 * 构建 QueryWrapper (基于 Entity).
	 * @param condition 查询条件
	 * @param entityClass 实体类
	 * @param <T> 实体类型
	 * @return QueryWrapper
	 */
	public static <T> QueryWrapper<T> build(QueryCondition condition, Class<T> entityClass) {
		QueryWrapper<T> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();
		Set<String> validFields = getValidFields(entityClass);

		if (!CollectionUtils.isEmpty(condition.getSelect())) {
			applySelect(wrapper, condition.getSelect(), validFields, entityClass);
		}

		Function<String, String> columnResolver = (field) -> {
			if (!validFields.contains(field)) {
				log.debug("Field {} does not exist in entity {}, ignored", field, entityClass.getSimpleName());
				return null;
			}
			return getColumnName(entityClass, field);
		};

		applyConditions(wrapper, condition, columnResolver);
		return wrapper;
	}

	/**
	 * 构建 QueryWrapper (基于 Map 映射).
	 * @param condition 查询条件
	 * @param fieldMapping 字段名到数据库列名的映射
	 * @return QueryWrapper
	 */
	public static QueryWrapper<Void> build(QueryCondition condition, Map<String, String> fieldMapping) {
		QueryWrapper<Void> wrapper = new QueryWrapper<>();
		wrapper.checkSqlInjection();

		Function<String, String> columnResolver = (field) -> {
			String column = fieldMapping.get(field);
			if (column == null) {
				log.debug("Field {} not found in mapping, ignored", field);
			}
			return column;
		};

		applyConditions(wrapper, condition, columnResolver);
		return wrapper;
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
		if (!ObjectUtils.isEmpty(condition.getWhere())) {
			applyWhere(wrapper, condition.getWhere(), columnResolver);
		}

		if (!CollectionUtils.isEmpty(condition.getOrderBy())) {
			applyOrderBy(wrapper, condition.getOrderBy(), columnResolver);
		}
	}

	private static <T> void applyWhere(QueryWrapper<T> wrapper, Map<?, ?> where,
			Function<String, String> columnResolver) {
		where.forEach((key, value) -> {
			if (!(key instanceof String k)) {
				return;
			}
			QueryOperator logicOp = QueryOperator.fromValue(k);
			if (logicOp == QueryOperator.AND) {
				if (value instanceof List<?> list) {
					wrapper.and((w) -> list.forEach((item) -> {
						if (item instanceof Map<?, ?> map) {
							applyWhere(w, map, columnResolver);
						}
					}));
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
					wrapper.not((w) -> applyWhere(w, map, columnResolver));
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
		});
	}

	private static <T> void applyOrConditions(QueryWrapper<T> wrapper, List<?> list,
			Function<String, String> columnResolver) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i) instanceof Map<?, ?> map) {
				if (i == 0) {
					wrapper.and((w) -> applyWhere(w, map, columnResolver));
				}
				else {
					wrapper.or((w) -> applyWhere(w, map, columnResolver));
				}
			}
		}
	}

	private static <T> void applyOperator(QueryWrapper<T> wrapper, String column, String operatorStr, Object value) {
		QueryOperator operator = QueryOperator.fromValue(operatorStr);
		if (operator == null) {
			log.warn("Unknown operator: {}, ignored", operatorStr);
			return;
		}

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

}
