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
import java.util.Optional;

import org.jax.snack.framework.mybatisplus.repository.BaseRepository;
import org.jax.snack.lowcode.biz.entity.LowcodeBusinessFlow;

/**
 * 业务流程仓储接口.
 *
 * @author Jax Jiang
 */
public interface LowcodeBusinessFlowRepository extends BaseRepository<LowcodeBusinessFlow, Long> {

	/**
	 * 根据流程编码查询.
	 * @param flowCode 流程编码
	 * @return 业务流程
	 */
	Optional<LowcodeBusinessFlow> findByFlowCode(String flowCode);

	/**
	 * 查询所有启用的业务流程.
	 * @return 业务流程列表
	 */
	List<LowcodeBusinessFlow> findAllEnabled();

}
