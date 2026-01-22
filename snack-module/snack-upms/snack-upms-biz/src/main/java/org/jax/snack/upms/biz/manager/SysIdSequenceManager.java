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

package org.jax.snack.upms.biz.manager;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.upms.biz.entity.SysIdSequence;
import org.jax.snack.upms.biz.repository.SysIdSequenceRepository;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * ID 序列号管理器.
 * <p>
 * 负责序列号的并发生成和周期管理.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysIdSequenceManager {

	private final SysIdSequenceRepository repository;

	/**
	 * 获取下一个序列号.
	 * @param ruleId 规则 ID
	 * @param cycleKey 周期标识
	 * @return 下一个序列号
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public long getNextValue(Long ruleId, String cycleKey) {
		try {
			SysIdSequence initialSeq = new SysIdSequence();
			initialSeq.setRuleId(ruleId);
			initialSeq.setCycleKey(cycleKey);
			initialSeq.setCurrentValue(0L);
			this.repository.save(initialSeq);
		}
		catch (DuplicateKeyException ignored) {
		}

		QueryCondition condition = QueryCondition.builder()
			.eq(SysIdSequence.Fields.ruleId, ruleId)
			.eq(SysIdSequence.Fields.cycleKey, cycleKey)
			.last("FOR UPDATE")
			.build();

		Optional<SysIdSequence> optional = this.repository.queryListByDsl(condition).stream().findFirst();

		if (optional.isEmpty()) {
			return getNextValue(ruleId, cycleKey);
		}

		SysIdSequence sequence = optional.get();
		long nextVal = sequence.getCurrentValue() + 1;
		sequence.setCurrentValue(nextVal);

		this.repository.update(sequence);

		return nextVal;
	}

}
