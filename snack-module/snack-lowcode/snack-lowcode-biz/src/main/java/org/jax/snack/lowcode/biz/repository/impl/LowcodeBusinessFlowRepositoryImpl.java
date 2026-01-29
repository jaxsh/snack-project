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
import org.jax.snack.lowcode.biz.entity.LowcodeBusinessFlow;
import org.jax.snack.lowcode.biz.mapper.LowcodeBusinessFlowMapper;
import org.jax.snack.lowcode.biz.repository.LowcodeBusinessFlowRepository;

import org.springframework.stereotype.Repository;

/**
 * 业务流程仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
public class LowcodeBusinessFlowRepositoryImpl
		extends AbstractRepository<LowcodeBusinessFlow, Long, LowcodeBusinessFlowMapper>
		implements LowcodeBusinessFlowRepository {

	@Override
	public Optional<LowcodeBusinessFlow> findByFlowCode(String flowCode) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeBusinessFlow.Fields.flowCode, flowCode).build();
		List<LowcodeBusinessFlow> list = queryListByDsl(condition);
		return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
	}

	@Override
	public List<LowcodeBusinessFlow> findAllEnabled() {
		QueryCondition condition = QueryCondition.builder().eq(LowcodeBusinessFlow.Fields.enabled, true).build();
		return queryListByDsl(condition);
	}

}
