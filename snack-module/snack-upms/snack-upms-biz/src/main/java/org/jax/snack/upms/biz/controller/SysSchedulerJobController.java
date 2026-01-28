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

package org.jax.snack.upms.biz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysSchedulerJobDTO;
import org.jax.snack.upms.api.vo.SysSchedulerJobLogVO;
import org.jax.snack.upms.api.vo.SysSchedulerJobVO;
import org.jax.snack.upms.biz.entity.SysSchedulerJob;
import org.jax.snack.upms.biz.entity.SysSchedulerJobLog;
import org.jax.snack.upms.biz.service.SysSchedulerJobService;
import org.quartz.SchedulerException;
import tools.jackson.core.JacksonException;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/scheduler-jobs")
@RequiredArgsConstructor
public class SysSchedulerJobController {

	private final SysSchedulerJobService schedulerJobService;

	/**
	 * 创建定时任务.
	 * @param dto 定时任务 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysSchedulerJobDTO dto)
			throws SchedulerException, ClassNotFoundException, JacksonException {
		this.schedulerJobService.create(dto);
	}

	/**
	 * 按 ID 查询定时任务.
	 * @param id 定时任务 ID
	 * @return 分页结果 (单条数据)
	 */
	@GetMapping("/{id}")
	public PageResult<SysSchedulerJobVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.id, id).build();
		return this.schedulerJobService.queryByDsl(condition);
	}

	/**
	 * 按条件查询定时任务.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysSchedulerJobVO> query(@RequestBody QueryCondition condition) {
		return this.schedulerJobService.queryByDsl(condition);
	}

	/**
	 * 更新定时任务.
	 * @param id 定时任务 ID.
	 * @param dto 定时任务 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysSchedulerJobDTO dto)
			throws SchedulerException, ClassNotFoundException, JacksonException {
		this.schedulerJobService.update(id, dto);
	}

	/**
	 * 删除定时任务.
	 * @param ids 定时任务 ID 列表.
	 */
	@DeleteMapping("/{ids}")
	public void delete(@PathVariable List<Long> ids) throws SchedulerException {
		WhereCondition condition = WhereCondition.builder().in(SysSchedulerJob.Fields.id, ids).build();
		this.schedulerJobService.deleteByDsl(condition);
	}

	/**
	 * 暂停定时任务.
	 * @param id 任务 ID.
	 */
	@PutMapping("/{id}/pause")
	public void pause(@PathVariable Long id) throws SchedulerException {
		this.schedulerJobService.pause(id);
	}

	/**
	 * 恢复定时任务.
	 * @param id 任务 ID.
	 */
	@PutMapping("/{id}/resume")
	public void resume(@PathVariable Long id) throws SchedulerException {
		this.schedulerJobService.resume(id);
	}

	/**
	 * 手动触发定时任务.
	 * @param id 任务 ID.
	 */
	@PostMapping("/{id}/trigger")
	public void trigger(@PathVariable Long id) throws SchedulerException {
		this.schedulerJobService.trigger(id);
	}

	/**
	 * 查询任务执行日志.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/logs/query")
	public PageResult<SysSchedulerJobLogVO> queryLogs(@RequestBody QueryCondition condition) {
		return this.schedulerJobService.queryLogsByDsl(condition);
	}

	/**
	 * 按任务 ID 查询执行日志.
	 * @param jobId 任务 ID
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/{jobId}/logs/query")
	public PageResult<SysSchedulerJobLogVO> queryLogsByJobId(@PathVariable Long jobId,
			@RequestBody QueryCondition condition) {
		QueryCondition finalCondition = QueryCondition.builder()
			.where(condition.getWhere())
			.eq(SysSchedulerJobLog.Fields.jobId, jobId)
			.current(condition.getCurrent())
			.size(condition.getSize())
			.orderBy(condition.getOrderBy())
			.build();
		return this.schedulerJobService.queryLogsByDsl(finalCondition);
	}

}
