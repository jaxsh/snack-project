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

package org.jax.snack.framework.common.result;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 分页结果对象.
 *
 * @param <T> 数据类型
 * @author Jax Jiang
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

	/**
	 * 数据列表.
	 */
	private List<T> records;

	/**
	 * 总记录数.
	 */
	private long total;

	/**
	 * 每页大小.
	 */
	private long size;

	/**
	 * 当前页码.
	 */
	private long current;

	/**
	 * 总页数.
	 */
	private long pages;

	/**
	 * 是否有上一页.
	 * @return 是否有上一页
	 */
	public boolean hasPrevious() {
		return this.current > 1;
	}

	/**
	 * 是否有下一页.
	 * @return 是否有下一页
	 */
	public boolean hasNext() {
		return this.current < this.pages;
	}

	/**
	 * 转换 PageResult 的泛型类型.
	 * @param mapper 转换函数
	 * @param <R> 目标泛型
	 * @return 新的 PageResult
	 */
	public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
		List<R> newRecords;
		if (this.records == null || this.records.isEmpty()) {
			newRecords = Collections.emptyList();
		}
		else {
			newRecords = this.records.stream().map(mapper).collect(Collectors.toList());
		}
		return new PageResult<>(newRecords, this.total, this.size, this.current, this.pages);
	}

	/**
	 * 创建空的分页结果.
	 * @param <T> 数据类型
	 * @return 空的分页结果
	 */
	public static <T> PageResult<T> empty() {
		return new PageResult<>(Collections.emptyList(), 0, 0, 1, 0);
	}

}
