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

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.mybatisplus.repository.AbstractRepository;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowChain;
import org.jax.snack.lowcode.biz.mapper.LowcodeFlowChainMapper;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowChainRepository;

import org.springframework.stereotype.Repository;

/**
 * 流程 Chain 仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
public class LowcodeFlowChainRepositoryImpl extends AbstractRepository<LowcodeFlowChain, Long, LowcodeFlowChainMapper>
		implements LowcodeFlowChainRepository {

	@Override
	public Optional<LowcodeFlowChain> findByFlowId(Long flowId) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeFlowChain.Fields.flowId, flowId).build();
		List<LowcodeFlowChain> list = queryListByDsl(condition);
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

}
