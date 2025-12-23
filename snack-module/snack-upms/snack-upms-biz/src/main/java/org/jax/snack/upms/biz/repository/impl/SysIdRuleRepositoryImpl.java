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
import org.jax.snack.upms.biz.entity.SysIdRule;
import org.jax.snack.upms.biz.mapper.SysIdRuleMapper;
import org.jax.snack.upms.biz.repository.SysIdRuleRepository;

import org.springframework.stereotype.Repository;

/**
 * ID 规则仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class SysIdRuleRepositoryImpl implements SysIdRuleRepository {

	private final SysIdRuleMapper mapper;

	@Override
	public boolean existsByDsl(QueryCondition condition) {
		QueryWrapper<SysIdRule> wrapper = QueryWrapperBuilder.build(condition, SysIdRule.class);
		return this.mapper.exists(wrapper);
	}

	@Override
	public void save(SysIdRule entity) {
		this.mapper.insert(entity);
	}

	@Override
	public Optional<SysIdRule> findById(Long id) {
		return Optional.ofNullable(this.mapper.selectById(id));
	}

	@Override
	public Optional<SysIdRule> findByRuleCode(String ruleCode) {
		LambdaQueryWrapper<SysIdRule> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SysIdRule::getRuleCode, ruleCode);
		return Optional.ofNullable(this.mapper.selectOne(wrapper));
	}

	@Override
	public void update(SysIdRule entity) {
		this.mapper.updateById(entity);
	}

	@Override
	public void deleteById(Long id) {
		this.mapper.deleteById(id);
	}

	@Override
	public Page<SysIdRule> queryPageByDsl(QueryCondition condition) {
		QueryWrapper<SysIdRule> wrapper = QueryWrapperBuilder.build(condition, SysIdRule.class);
		long current = (condition.getCurrent() != null) ? condition.getCurrent() : 1L;
		Page<SysIdRule> page = new Page<>(current, condition.getSize());
		return this.mapper.selectPage(page, wrapper);
	}

	@Override
	public List<SysIdRule> queryListByDsl(QueryCondition condition) {
		QueryWrapper<SysIdRule> wrapper = QueryWrapperBuilder.build(condition, SysIdRule.class);
		return this.mapper.selectList(wrapper);
	}

}
