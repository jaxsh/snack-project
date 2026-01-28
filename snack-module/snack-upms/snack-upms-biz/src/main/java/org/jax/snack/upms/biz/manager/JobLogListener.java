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

package org.jax.snack.upms.biz.manager;

import java.io.PrintWriter;
import java.io.StringWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.enums.SuccessStatus;
import org.jax.snack.upms.biz.entity.SysSchedulerJob;
import org.jax.snack.upms.biz.entity.SysSchedulerJobLog;
import org.jax.snack.upms.biz.repository.SysSchedulerJobLogRepository;
import org.jax.snack.upms.biz.repository.SysSchedulerJobRepository;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 任务执行监听器.
 * <p>
 * 记录任务执行日志.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogListener implements JobListener {

	private static final String LISTENER_NAME = "SchedulerJobLogListener";

	private static final String KEY_STOPWATCH = "_stopWatch";

	private static final String KEY_LOG_ID = "_logId";

	private final SysSchedulerJobRepository jobRepository;

	private final SysSchedulerJobLogRepository logRepository;

	@Override
	public String getName() {
		return LISTENER_NAME;
	}

	@Override
	public void jobToBeExecuted(JobExecutionContext context) {
		String jobName = context.getJobDetail().getKey().getName();

		StopWatch sw = new StopWatch();
		sw.start();
		context.put(KEY_STOPWATCH, sw);

		SysSchedulerJobLog logRecord = new SysSchedulerJobLog();
		logRecord.setJobName(jobName);

		QueryCondition condition = QueryCondition.builder().eq(SysSchedulerJob.Fields.jobName, jobName).build();
		this.jobRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.ifPresent((job) -> logRecord.setJobId(job.getId()));

		logRecord.setStatus(SuccessStatus.FAIL.getCode());

		this.logRepository.save(logRecord);
		context.put(KEY_LOG_ID, logRecord.getId());
	}

	@Override
	public void jobExecutionVetoed(JobExecutionContext context) {
		log.warn("Job execution vetoed: {}", context.getJobDetail().getKey());
	}

	@Override
	public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
		Long logId = (Long) context.get(KEY_LOG_ID);
		if (logId == null) {
			return;
		}

		StopWatch sw = (StopWatch) context.get(KEY_STOPWATCH);
		if (sw != null && sw.isRunning()) {
			sw.stop();
		}

		this.logRepository.findById(logId).ifPresent((logRecord) -> {
			if (sw != null) {
				logRecord.setDuration(sw.getTotalTimeMillis());
			}

			if (jobException != null) {
				logRecord.setStatus(SuccessStatus.FAIL.getCode());
				logRecord.setErrorMessage(getStackTrace(jobException));
				log.error("Job execution failed: {}", logRecord.getJobName(), jobException);
			}
			else {
				logRecord.setStatus(SuccessStatus.SUCCESS.getCode());
			}

			this.logRepository.update(logRecord);
		});
	}

	private String getStackTrace(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

}
