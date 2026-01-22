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

package org.jax.snack.upms.biz.service;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysSchedulerJobDTO;
import org.jax.snack.upms.api.vo.SysSchedulerJobLogVO;
import org.jax.snack.upms.api.vo.SysSchedulerJobVO;
import org.quartz.SchedulerException;
import tools.jackson.core.JacksonException;

/**
 * 定时任务服务接口.
 *
 * @author Jax Jiang
 */
public interface SysSchedulerJobService {

	/**
	 * 创建定时任务.
	 * @param dto 定时任务 DTO.
	 */
	void create(SysSchedulerJobDTO dto) throws SchedulerException, ClassNotFoundException, JacksonException;

	/**
	 * 更新定时任务.
	 * @param id 主键 ID.
	 * @param dto 定时任务 DTO.
	 */
	void update(Long id, SysSchedulerJobDTO dto) throws SchedulerException, ClassNotFoundException, JacksonException;

	/**
	 * 根据条件删除定时任务.
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition) throws SchedulerException;

	/**
	 * 使用 JSON DSL 查询定时任务.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysSchedulerJobVO> queryByDsl(QueryCondition condition);

	/**
	 * 暂停定时任务.
	 * @param id 任务 ID.
	 */
	void pause(Long id) throws SchedulerException;

	/**
	 * 恢复定时任务.
	 * @param id 任务 ID.
	 */
	void resume(Long id) throws SchedulerException;

	/**
	 * 手动触发定时任务.
	 * @param id 任务 ID.
	 */
	void trigger(Long id) throws SchedulerException;

	/**
	 * 查询任务执行日志.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysSchedulerJobLogVO> queryLogsByDsl(QueryCondition condition);

}
