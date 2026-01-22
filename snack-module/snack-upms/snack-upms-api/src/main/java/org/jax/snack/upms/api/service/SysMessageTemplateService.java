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

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysMessageTemplateDTO;
import org.jax.snack.upms.api.vo.SysMessageTemplateVO;

/**
 * 消息模版 Service 接口.
 *
 * @author Jax Jiang
 */
public interface SysMessageTemplateService {

	/**
	 * 创建消息模版.
	 * @param dto 消息模版 DTO
	 */
	void create(SysMessageTemplateDTO dto);

	/**
	 * 更新消息模版.
	 * @param id ID
	 * @param dto 消息模版 DTO
	 */
	void update(Long id, SysMessageTemplateDTO dto);

	/**
	 * 根据 DSL 删除消息模版.
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 根据 DSL 查询消息模版.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysMessageTemplateVO> queryByDsl(QueryCondition condition);

	/**
	 * 根据 ID 获取消息模版.
	 * @param id ID
	 * @return 详情
	 */
	SysMessageTemplateVO getById(Long id);

}
