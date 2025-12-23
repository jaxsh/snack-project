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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.biz.entity.SysIdSequence;
import org.jax.snack.upms.biz.mapper.SysIdSequenceMapper;
import org.jax.snack.upms.biz.repository.SysIdSequenceRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ID 序列号仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
@RequiredArgsConstructor
public class SysIdSequenceRepositoryImpl implements SysIdSequenceRepository {

	private final SysIdSequenceMapper mapper;

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public long getNextValue(Long ruleId, String cycleKey) {
		try {
			SysIdSequence initialSeq = new SysIdSequence();
			initialSeq.setRuleId(ruleId);
			initialSeq.setCycleKey(cycleKey);
			initialSeq.setCurrentValue(0L);
			this.mapper.insert(initialSeq);
		}
		catch (DuplicateKeyException ignored) {
		}

		LambdaQueryWrapper<SysIdSequence> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(SysIdSequence::getRuleId, ruleId).eq(SysIdSequence::getCycleKey, cycleKey).last("FOR UPDATE");

		SysIdSequence sequence = this.mapper.selectOne(queryWrapper);

		if (sequence == null) {
			return getNextValue(ruleId, cycleKey);
		}

		long nextVal = sequence.getCurrentValue() + 1;
		sequence.setCurrentValue(nextVal);
		this.mapper.updateById(sequence);

		return nextVal;
	}

	@Override
	public void deleteByRuleId(Long ruleId) {
		LambdaQueryWrapper<SysIdSequence> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(SysIdSequence::getRuleId, ruleId);
		this.mapper.delete(wrapper);
	}

}
