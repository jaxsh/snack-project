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
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.mybatisplus.query.QueryWrapperBuilder;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.jax.snack.lowcode.biz.mapper.LowcodeSchemaMapper;
import org.jax.snack.lowcode.biz.repository.LowcodeSchemaRepository;

import org.springframework.stereotype.Repository;

/**
 * Schema 仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class LowcodeSchemaRepositoryImpl implements LowcodeSchemaRepository {

	private final LowcodeSchemaMapper mapper;

	@Override
	public void save(LowcodeSchema entity) {
		this.mapper.insert(entity);
	}

	@Override
	public void update(LowcodeSchema entity) {
		this.mapper.updateById(entity);
	}

	@Override
	public Optional<LowcodeSchema> findById(Long id) {
		return Optional.ofNullable(this.mapper.selectById(id));
	}

	@Override
	public void deleteById(Long id) {
		this.mapper.deleteById(id);
	}

	@Override
	public boolean existsByDsl(QueryCondition condition) {
		QueryWrapper<LowcodeSchema> wrapper = QueryWrapperBuilder.build(condition, LowcodeSchema.class);
		return this.mapper.exists(wrapper);
	}

	@Override
	public Page<LowcodeSchema> queryPageByDsl(QueryCondition condition) {
		QueryWrapper<LowcodeSchema> wrapper = QueryWrapperBuilder.build(condition, LowcodeSchema.class);
		long current = (condition.getCurrent() != null) ? condition.getCurrent() : 1L;
		Page<LowcodeSchema> page = new Page<>(current, condition.getSize());
		return this.mapper.selectPage(page, wrapper);
	}

	@Override
	public List<LowcodeSchema> queryListByDsl(QueryCondition condition) {
		QueryWrapper<LowcodeSchema> wrapper = QueryWrapperBuilder.build(condition, LowcodeSchema.class);
		return this.mapper.selectList(wrapper);
	}

	@Override
	public void deleteBySchemaName(String schemaName) {
		this.mapper.delete(new LambdaQueryWrapper<LowcodeSchema>().eq(LowcodeSchema::getSchemaName, schemaName));
	}

}
