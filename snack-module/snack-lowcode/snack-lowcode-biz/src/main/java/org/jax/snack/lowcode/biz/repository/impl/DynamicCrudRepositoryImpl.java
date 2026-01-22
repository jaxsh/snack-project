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

package org.jax.snack.lowcode.biz.repository.impl;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.jax.snack.lowcode.biz.mapper.DynamicCrudMapper;
import org.jax.snack.lowcode.biz.repository.DynamicCrudRepository;

import org.springframework.stereotype.Repository;

/**
 * 动态 CRUD 仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class DynamicCrudRepositoryImpl implements DynamicCrudRepository {

	private final DynamicCrudMapper mapper;

	@Override
	public IPage<Map<String, Object>> selectPage(IPage<Map<String, Object>> page, String selectColumns,
			Wrapper<Void> wrapper) {
		return this.mapper.dynamicSelectPage(page, selectColumns, wrapper);
	}

	@Override
	public List<Map<String, Object>> selectList(String selectColumns, Wrapper<Void> wrapper) {
		return this.mapper.dynamicSelectList(selectColumns, wrapper);
	}

	@Override
	public int insert(Map<String, Object> data) {
		return this.mapper.dynamicInsert(data);
	}

	@Override
	public int update(Wrapper<Void> wrapper) {
		return this.mapper.dynamicUpdate(wrapper);
	}

	@Override
	public int delete(Wrapper<Void> wrapper) {
		return this.mapper.dynamicDelete(wrapper);
	}

}
