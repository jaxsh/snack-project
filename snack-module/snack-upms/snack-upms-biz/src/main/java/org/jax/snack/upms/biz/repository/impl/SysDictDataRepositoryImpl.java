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

package org.jax.snack.upms.biz.repository.impl;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.mybatisplus.query.QueryWrapperBuilder;
import org.jax.snack.upms.biz.entity.SysDictData;
import org.jax.snack.upms.biz.mapper.SysDictDataMapper;
import org.jax.snack.upms.biz.repository.SysDictDataRepository;

import org.springframework.stereotype.Repository;

/**
 * 字典数据仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class SysDictDataRepositoryImpl implements SysDictDataRepository {

	private final SysDictDataMapper mapper;

	@Override
	public boolean existsByDsl(QueryCondition condition) {
		QueryWrapper<SysDictData> wrapper = QueryWrapperBuilder.build(condition, SysDictData.class);
		return this.mapper.exists(wrapper);
	}

	@Override
	public void save(SysDictData entity) {
		this.mapper.insert(entity);
	}

	@Override
	public Optional<SysDictData> findById(Long id) {
		return Optional.ofNullable(this.mapper.selectById(id));
	}

	@Override
	public void update(SysDictData entity) {
		this.mapper.updateById(entity);
	}

	@Override
	public void deleteById(Long id) {
		this.mapper.deleteById(id);
	}

	@Override
	public Page<SysDictData> queryPageByDsl(QueryCondition condition) {
		QueryWrapper<SysDictData> wrapper = QueryWrapperBuilder.build(condition, SysDictData.class);
		long current = (condition.getCurrent() != null) ? condition.getCurrent() : 1L;
		Page<SysDictData> page = new Page<>(current, condition.getSize());
		return this.mapper.selectPage(page, wrapper);
	}

	@Override
	public List<SysDictData> queryListByDsl(QueryCondition condition) {
		QueryWrapper<SysDictData> wrapper = QueryWrapperBuilder.build(condition, SysDictData.class);
		return this.mapper.selectList(wrapper);
	}

	@Override
	public void deleteByDictType(String dictType) {
		this.mapper.delete(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, dictType));
	}

	@Override
	public void saveOrUpdateBatch(List<SysDictData> entities) {
		entities.forEach((entity) -> {
			SysDictData existing = this.mapper
				.selectOne(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getDictType, entity.getDictType())
					.eq(SysDictData::getDictValue, entity.getDictValue())
					.last("LIMIT 1"));
			if (existing != null) {
				entity.setId(existing.getId());
				this.mapper.updateById(entity);
			}
			else {
				this.mapper.insert(entity);
			}
		});
	}

}
