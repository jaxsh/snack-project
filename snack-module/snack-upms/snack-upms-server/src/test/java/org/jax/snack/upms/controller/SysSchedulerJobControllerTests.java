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

package org.jax.snack.upms.controller;

import java.time.ZonedDateTime;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysSchedulerJobDTO;
import org.jax.snack.upms.api.vo.SysSchedulerJobVO;
import org.jax.snack.upms.biz.entity.SysSchedulerJob;
import org.jax.snack.upms.biz.service.SysSchedulerJobService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 定时任务 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysSchedulerJobControllerTests extends UpmsIntegrationTests {

	private static final String API_SCHEDULER_JOBS = "/api/upms/scheduler-jobs";

	private static final String API_SCHEDULER_JOBS_QUERY = "/api/upms/scheduler-jobs/query";

	private static final String API_SCHEDULER_JOBS_ID = "/api/upms/scheduler-jobs/{id}";

	private static final String API_SCHEDULER_JOBS_IDS = "/api/upms/scheduler-jobs/{ids}";

	private static final String API_SCHEDULER_JOBS_PAUSE = "/api/upms/scheduler-jobs/{id}/pause";

	private static final String API_SCHEDULER_JOBS_RESUME = "/api/upms/scheduler-jobs/{id}/resume";

	private static final String API_SCHEDULER_JOBS_TRIGGER = "/api/upms/scheduler-jobs/{id}/trigger";

	private static final String DEMO_JOB_CLASS = SysSchedulerJobControllerTests.DemoJob.class.getName();

	@Autowired
	private SysSchedulerJobService schedulerJobService;

	private SysSchedulerJobDTO buildDto(String name, String cronExpression, Integer status) {
		SysSchedulerJobDTO dto = new SysSchedulerJobDTO();
		dto.setJobName(name);
		dto.setJobClassName(DEMO_JOB_CLASS);
		dto.setCronExpression(cronExpression);
		dto.setStatus(status);
		dto.setDescription("Test job");
		return dto;
	}

	private SysSchedulerJobVO queryByJobName(String jobName) {
		QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.jobName, jobName).build();
		PageResult<SysSchedulerJobVO> result = this.schedulerJobService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("SchedulerJob not found: " + jobName);
		}
		return result.getRecords().get(0);
	}

	private SysSchedulerJobVO queryById(Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.id, id).build();
		PageResult<SysSchedulerJobVO> result = this.schedulerJobService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("SchedulerJob not found: " + id);
		}
		return result.getRecords().get(0);
	}

	private boolean existsById(Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.id, id).build();
		return !this.schedulerJobService.queryByDsl(condition).getRecords().isEmpty();
	}

	@Nested
	class CreateSchedulerJob {

		@Test
		void shouldCreateAndVerifyData() throws Exception {
			String jobName = "test_job_" + System.currentTimeMillis();
			SysSchedulerJobDTO dto = buildDto(jobName, "0 0/1 * * * ?", Status.ENABLED.getCode());

			postJson(API_SCHEDULER_JOBS, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.jobName, jobName).build();
			postJson(API_SCHEDULER_JOBS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".jobName", jobName))
				.andExpect(PageResultMatchers.record(0, ".status", Status.ENABLED.getCode()));
		}

		@Test
		void shouldFailWhenJobNameBlank() throws Exception {
			SysSchedulerJobDTO dto = new SysSchedulerJobDTO();
			dto.setJobClassName(DEMO_JOB_CLASS);
			dto.setCronExpression("0 0/2 * * * ?");
			dto.setStatus(Status.ENABLED.getCode());

			postJson(API_SCHEDULER_JOBS, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("jobName"));
		}

		@Test
		void shouldFailWhenDuplicateJobName() throws Exception {
			String jobName = "duplicate_job_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 0/3 * * * ?", Status.DISABLED.getCode()));

			SysSchedulerJobDTO dto = buildDto(jobName, "0 0/10 * * * ?", Status.ENABLED.getCode());

			postJson(API_SCHEDULER_JOBS, dto).andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_ALREADY_EXISTS));
		}

	}

	@Nested
	class GetSchedulerJobById {

		@Test
		void shouldReturnSchedulerJobWithCorrectFields() throws Exception {
			String jobName = "get_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 0/4 * * * ?", Status.ENABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			getJson(API_SCHEDULER_JOBS_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".jobName", jobName))
				.andExpect(PageResultMatchers.record(0, ".status", 1));
		}

		@Test
		void shouldReturnEmptyWhenNotFound() throws Exception {
			getJson(API_SCHEDULER_JOBS_ID, 99999L).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(0));
		}

	}

	@Nested
	class QuerySchedulerJobs {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto("page1_" + System.currentTimeMillis(), "0 0/5 * * * ?", Status.ENABLED.getCode()));
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto("page2_" + System.currentTimeMillis(), "0 0/10 * * * ?", Status.ENABLED.getCode()));

			QueryCondition condition = QueryCondition.builder().current(1).size(10).build();

			postJson(API_SCHEDULER_JOBS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.isNotEmpty());
		}

	}

	@Nested
	class UpdateSchedulerJob {

		@Test
		void shouldUpdateAndVerifyData() throws Exception {
			String jobName = "update_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 0/7 * * * ?", Status.ENABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			SysSchedulerJobDTO dto = new SysSchedulerJobDTO();
			dto.setCronExpression("0 0/15 * * * ?");
			dto.setDescription("Updated description");

			putJson(API_SCHEDULER_JOBS_ID, dto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysSchedulerJobVO updated = queryById(id);
			assertThat(updated.getCronExpression()).isEqualTo("0 0/15 * * * ?");
			assertThat(updated.getDescription()).isEqualTo("Updated description");
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			SysSchedulerJobDTO dto = new SysSchedulerJobDTO();
			dto.setDescription("Updated description");

			putJson(API_SCHEDULER_JOBS_ID, dto, 99999L).andDo(print())
				.andExpect(status().is(422))
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class DeleteSchedulerJob {

		@Test
		void shouldDeleteAndVerifyRemoved() throws Exception {
			String jobName = "delete_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 0/8 * * * ?", Status.DISABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			assertThat(existsById(id)).isTrue();

			deleteJson(API_SCHEDULER_JOBS_IDS, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(existsById(id)).isFalse();
		}

	}

	@Nested
	class PauseAndResumeJob {

		@Test
		void shouldPauseJob() throws Exception {
			String jobName = "pause_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 0/9 * * * ?", Status.ENABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			putJson(API_SCHEDULER_JOBS_PAUSE, Map.of(), id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysSchedulerJobVO updated = queryById(id);
			assertThat(updated.getStatus()).isEqualTo(Status.DISABLED.getCode());
		}

		@Test
		void shouldResumeJob() throws Exception {
			String jobName = "resume_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 1/5 * * * ?", Status.DISABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			putJson(API_SCHEDULER_JOBS_RESUME, Map.of(), id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysSchedulerJobVO updated = queryById(id);
			assertThat(updated.getStatus()).isEqualTo(Status.ENABLED.getCode());
		}

	}

	@Nested
	class TriggerJob {

		@Test
		void shouldTriggerJob() throws Exception {
			String jobName = "trigger_test_" + System.currentTimeMillis();
			SysSchedulerJobControllerTests.this.schedulerJobService
				.create(buildDto(jobName, "0 2/5 * * * ?", Status.ENABLED.getCode()));
			Long id = queryByJobName(jobName).getId();

			postJson(API_SCHEDULER_JOBS_TRIGGER, Map.of(), id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());
		}

	}

	/**
	 * 示例定时任务 (测试用).
	 */
	@Slf4j
	public static class DemoJob implements Job {

		@Override
		public void execute(JobExecutionContext context) {
			log.info("DemoJob executed at: {}", ZonedDateTime.now());
		}

	}

}
