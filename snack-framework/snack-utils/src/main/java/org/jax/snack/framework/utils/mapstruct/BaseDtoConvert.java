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

package org.jax.snack.framework.utils.mapstruct;

import java.util.List;

import org.jax.snack.framework.core.api.result.PageResult;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * MapStruct 通用转换接口.
 *
 * @param <D> DTO 类型 (Data Transfer Object)
 * @param <E> Entity 类型 (Domain Object)
 * @param <V> VO 类型 (View Object)
 * @author Jax Jiang
 */
public interface BaseDtoConvert<D, E, V> {

	/**
	 * DTO 转 Entity.
	 * @param dto DTO 对象
	 * @return Entity 对象
	 */
	E toEntity(D dto);

	/**
	 * Entity 转 VO.
	 * @param entity Entity 对象
	 * @return VO 对象
	 */
	V toVO(E entity);

	/**
	 * DTO 列表转 Entity 列表.
	 * @param dtoList DTO 列表
	 * @return Entity 列表
	 */
	List<E> toEntity(List<D> dtoList);

	/**
	 * Entity 列表转 VO 列表.
	 * @param entityList Entity 列表
	 * @return VO 列表
	 */
	List<V> toVO(List<E> entityList);

	/**
	 * Entity 分页结果转 VO 分页结果.
	 * @param pageResult Entity 分页结果
	 * @return VO 分页结果
	 */
	default PageResult<V> toPageVO(PageResult<E> pageResult) {
		return pageResult.map(this::toVO);
	}

	/**
	 * JsonNullable 通用解包. MapStruct 自动用于所有 JsonNullable&lt;T&gt; 到 T 的映射.
	 * <p>
	 * present 返回容器内的值(含 null), undefined 返回 null. 注意: "显式 null"与"未传"解包后都为 null, 无法区分,
	 * 字段清空需由业务层另行处理.
	 * @param value JsonNullable 包装值
	 * @param <T> 值类型
	 * @return 解包后的值
	 */
	default <T> T unwrap(JsonNullable<T> value) {
		return (value != null) ? value.orElse(null) : null;
	}

}
