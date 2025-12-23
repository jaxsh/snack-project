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

package org.jax.snack.upms.api.service;

import java.util.Map;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.vo.SysIdRuleVO;

/**
 * ID 规则服务接口.
 *
 * @author Jax Jiang
 */
public interface SysIdRuleService {

	/**
	 * 创建规则.
	 * @param dto 规则 DTO
	 */
	void create(SysIdRuleDTO dto);

	/**
	 * 更新规则.
	 * @param id 规则 ID
	 * @param dto 规则 DTO
	 */
	void update(Long id, SysIdRuleDTO dto);

	/**
	 * 删除规则.
	 * @param id 规则 ID
	 */
	void deleteById(Long id);

	/**
	 * 根据 ID 查询规则详情 (含片段).
	 * @param id 规则 ID
	 * @return 规则 VO
	 */
	SysIdRuleVO getById(Long id);

	/**
	 * DSL 查询规则列表.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysIdRuleVO> queryByDsl(QueryCondition condition);

	/**
	 * 根据规则编码生成 ID.
	 * @param ruleCode 规则编码
	 * @return 生成的 ID
	 */
	String generate(String ruleCode);

	/**
	 * 根据规则编码生成 ID (带动态参数).
	 * @param ruleCode 规则编码
	 * @param args 动态参数
	 * @return 生成的 ID
	 */
	String generate(String ruleCode, Map<String, String> args);

}
