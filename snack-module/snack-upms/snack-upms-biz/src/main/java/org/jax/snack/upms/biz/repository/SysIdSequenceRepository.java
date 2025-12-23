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

package org.jax.snack.upms.biz.repository;

/**
 * ID 序列号仓储接口.
 *
 * @author Jax Jiang
 */
public interface SysIdSequenceRepository {

	/**
	 * 获取下一个序列号.
	 * <p>
	 * 如果周期记录不存在则自动创建.
	 * @param ruleId 规则 ID
	 * @param cycleKey 周期标识
	 * @return 下一个序列号
	 */
	long getNextValue(Long ruleId, String cycleKey);

	/**
	 * 根据规则 ID 删除所有序列号记录.
	 * @param ruleId 规则 ID
	 */
	void deleteByRuleId(Long ruleId);

}
