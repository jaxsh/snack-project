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

package org.jax.snack.framework.common.mapstruct;

import java.util.List;

import org.jax.snack.framework.common.result.PageResult;

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

}
