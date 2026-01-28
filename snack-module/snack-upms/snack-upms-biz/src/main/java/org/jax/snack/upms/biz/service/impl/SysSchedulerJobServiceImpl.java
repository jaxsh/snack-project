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

package org.jax.snack.upms.biz.service.impl;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysSchedulerJobDTO;
import org.jax.snack.upms.api.vo.SysSchedulerJobLogVO;
import org.jax.snack.upms.api.vo.SysSchedulerJobVO;
import org.jax.snack.upms.biz.converter.SysSchedulerJobConverter;
import org.jax.snack.upms.biz.converter.SysSchedulerJobLogConverter;
import org.jax.snack.upms.biz.entity.SysSchedulerJob;
import org.jax.snack.upms.biz.manager.SchedulerManager;
import org.jax.snack.upms.biz.repository.SysSchedulerJobLogRepository;
import org.jax.snack.upms.biz.repository.SysSchedulerJobRepository;
import org.jax.snack.upms.biz.service.SysSchedulerJobService;
import org.quartz.SchedulerException;
import tools.jackson.core.JacksonException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 定时任务服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysSchedulerJobServiceImpl implements SysSchedulerJobService {

	private static final String SCHEDULER_JOB = "Scheduler Job";

	private final SysSchedulerJobRepository repository;

	private final SysSchedulerJobLogRepository logRepository;

	private final SysSchedulerJobConverter converter;

	private final SysSchedulerJobLogConverter logConverter;

	private final SchedulerManager schedulerManager;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysSchedulerJobDTO dto) throws SchedulerException, ClassNotFoundException, JacksonException {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysSchedulerJob.Fields.jobName, dto.getJobName())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, SCHEDULER_JOB);
		}

		SysSchedulerJob entity = this.converter.toEntity(dto);
		this.repository.save(entity);

		boolean paused = Status.DISABLED.getCode().equals(dto.getStatus());
		this.schedulerManager.addJob(dto.getJobName(), dto.getJobClassName(), dto.getCronExpression(), dto.getJobData(),
				paused);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysSchedulerJobDTO dto)
			throws SchedulerException, ClassNotFoundException, JacksonException {
		SysSchedulerJob existing = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, SCHEDULER_JOB));

		SysSchedulerJob entity = this.converter.toEntity(dto);
		entity.setId(id);
		this.repository.update(entity);

		if (!ObjectUtils.nullSafeEquals(existing.getCronExpression(), dto.getCronExpression())
				&& dto.getCronExpression() != null) {
			this.schedulerManager.updateCron(existing.getJobName(), dto.getCronExpression());
		}

		if (!ObjectUtils.nullSafeEquals(existing.getJobData(), dto.getJobData())) {
			this.schedulerManager.updateJobData(existing.getJobName(), existing.getJobClassName(), dto.getJobData());
		}

		if (dto.getStatus() != null && !dto.getStatus().equals(existing.getStatus())) {
			if (Status.ENABLED.getCode().equals(dto.getStatus())) {
				this.schedulerManager.resumeJob(existing.getJobName());
			}
			else {
				this.schedulerManager.pauseJob(existing.getJobName());
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) throws SchedulerException {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		for (SysSchedulerJob entity : this.repository.queryListByDsl(queryCondition)) {
			this.schedulerManager.deleteJob(entity.getJobName());
		}
		this.repository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysSchedulerJobVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void pause(Long id) throws SchedulerException {
		SysSchedulerJob job = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, SCHEDULER_JOB));

		job.setStatus(Status.DISABLED.getCode());
		this.repository.update(job);
		this.schedulerManager.pauseJob(job.getJobName());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void resume(Long id) throws SchedulerException {
		SysSchedulerJob job = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, SCHEDULER_JOB));

		job.setStatus(Status.ENABLED.getCode());
		this.repository.update(job);
		this.schedulerManager.resumeJob(job.getJobName());
	}

	@Override
	public void trigger(Long id) throws SchedulerException {
		SysSchedulerJob job = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, SCHEDULER_JOB));

		this.schedulerManager.triggerJob(job.getJobName());
	}

	@Override
	public PageResult<SysSchedulerJobLogVO> queryLogsByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.logConverter.toPageResult(this.logRepository.queryPageByDsl(condition));
		}
		else {
			return this.logConverter.toPageResult(this.logRepository.queryListByDsl(condition));
		}
	}

}
