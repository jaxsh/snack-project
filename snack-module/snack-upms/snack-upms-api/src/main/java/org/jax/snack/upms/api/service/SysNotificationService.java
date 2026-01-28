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
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.vo.SysNotificationVO;

/**
 * 站内信 Service 接口.
 *
 * @author Jax Jiang
 */
public interface SysNotificationService {

	/**
	 * 分页查询站内信.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysNotificationVO> queryByDsl(QueryCondition condition);

	/**
	 * 删除站内信.
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 通用更新.
	 * @param data 更新数据
	 * @param condition 更新条件
	 */
	void updateByDsl(Map<String, Object> data, WhereCondition condition);

}
