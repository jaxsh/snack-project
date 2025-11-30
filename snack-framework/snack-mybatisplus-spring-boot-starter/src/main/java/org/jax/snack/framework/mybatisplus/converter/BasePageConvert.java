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

package org.jax.snack.framework.mybatisplus.converter;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jax.snack.framework.core.api.result.PageResult;

/**
 * MapStruct 分页转换基类.
 *
 * @param <S> 源类型 (Source, 通常是 DO)
 * @param <T> 目标类型 (Target, 通常是 DTO/VO)
 * @author Jax Jiang
 */
public interface BasePageConvert<S, T> {

	/**
	 * 列表转换 (由 MapStruct 自动生成实现).
	 * @param source 源列表
	 * @return 目标列表
	 */
	List<T> toList(List<S> source);

	/**
	 * 分页对象转分页结果.
	 * @param page MyBatis Plus 分页对象
	 * @return 分页结果
	 */
	default PageResult<T> toPageResult(IPage<S> page) {
		if (page == null) {
			return PageResult.empty();
		}
		PageResult<T> result = new PageResult<>();
		result.setCurrent(page.getCurrent());
		result.setSize(page.getSize());
		result.setTotal(page.getTotal());
		result.setPages(page.getPages());
		result.setRecords(toList(page.getRecords()));
		return result;
	}

	/**
	 * 列表转分页结果.
	 * @param list 源列表
	 * @return 分页结果
	 */
	default PageResult<T> toPageResult(List<S> list) {
		if (list == null || list.isEmpty()) {
			return PageResult.empty();
		}
		List<T> converted = toList(list);
		return new PageResult<>(converted, converted.size(), converted.size(), 1L, 1L);
	}

}
