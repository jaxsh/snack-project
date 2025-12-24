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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.mybatisplus.query.QueryWrapperBuilder;
import org.jax.snack.upms.biz.entity.SysOrg;
import org.jax.snack.upms.biz.mapper.SysOrgMapper;
import org.jax.snack.upms.biz.repository.SysOrgRepository;

import org.springframework.stereotype.Repository;

/**
 * 组织机构仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class SysOrgRepositoryImpl implements SysOrgRepository {

	private final SysOrgMapper mapper;

	@Override
	public boolean existsByDsl(QueryCondition condition) {
		QueryWrapper<SysOrg> wrapper = QueryWrapperBuilder.build(condition, SysOrg.class);
		return this.mapper.exists(wrapper);
	}

	@Override
	public void save(SysOrg entity) {
		this.mapper.insert(entity);
	}

	@Override
	public Optional<SysOrg> findById(Long id) {
		return Optional.ofNullable(this.mapper.selectById(id));
	}

	@Override
	public void update(SysOrg entity) {
		this.mapper.updateById(entity);
	}

	@Override
	public void deleteById(Long id) {
		this.mapper.deleteById(id);
	}

	@Override
	public void deleteByIds(List<Long> ids) {
		this.mapper.deleteByIds(ids);
	}

	@Override
	public Page<SysOrg> queryPageByDsl(QueryCondition condition) {
		QueryWrapper<SysOrg> wrapper = QueryWrapperBuilder.build(condition, SysOrg.class);
		long current = (condition.getCurrent() != null) ? condition.getCurrent() : 1L;
		Page<SysOrg> page = new Page<>(current, condition.getSize());
		return this.mapper.selectPage(page, wrapper);
	}

	@Override
	public List<SysOrg> queryListByDsl(QueryCondition condition) {
		QueryWrapper<SysOrg> wrapper = QueryWrapperBuilder.build(condition, SysOrg.class);
		return this.mapper.selectList(wrapper);
	}

	@Override
	public void batchUpdateDescendants(String oldPrefix, String newPrefix, int levelDiff) {
		LambdaUpdateWrapper<SysOrg> wrapper = Wrappers.<SysOrg>lambdaUpdate()
			.likeRight(SysOrg::getAncestors, oldPrefix)
			.setSql("ancestors = REPLACE(ancestors, '" + oldPrefix + "', '" + newPrefix + "')")
			.setSql("level = level + " + levelDiff);
		this.mapper.update(wrapper);
	}

	@Override
	public void batchUpdateDescendantsStatus(String ancestorsPrefix, Integer status) {
		LambdaUpdateWrapper<SysOrg> wrapper = Wrappers.<SysOrg>lambdaUpdate()
			.likeRight(SysOrg::getAncestors, ancestorsPrefix)
			.set(SysOrg::getStatus, status);
		this.mapper.update(wrapper);
	}

}
