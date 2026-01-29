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

package org.jax.snack.lowcode.biz.repository;

import java.util.List;

import org.jax.snack.framework.mybatisplus.repository.BaseRepository;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowStep;

/**
 * 流程步骤仓储接口.
 *
 * @author Jax Jiang
 */
public interface LowcodeFlowStepRepository extends BaseRepository<LowcodeFlowStep, Long> {

	/**
	 * 根据 Chain ID 查询所有步骤.
	 * @param chainId Chain ID
	 * @return 步骤列表
	 */
	List<LowcodeFlowStep> findByChainId(Long chainId);

	/**
	 * 根据 Chain ID 删除所有步骤.
	 * @param chainId Chain ID
	 */
	void deleteByChainId(Long chainId);

}
