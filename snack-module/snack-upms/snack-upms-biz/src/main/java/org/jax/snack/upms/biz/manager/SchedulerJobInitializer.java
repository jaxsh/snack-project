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

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.upms.api.enums.Status;
import org.jax.snack.upms.biz.entity.SysSchedulerJob;
import org.jax.snack.upms.biz.repository.SysSchedulerJobRepository;
import org.jspecify.annotations.NullMarked;
import org.quartz.Job;
import org.quartz.SchedulerException;
import tools.jackson.core.JacksonException;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * 定时任务初始化器.
 * <p>
 * 应用启动时自动加载数据库中的定时任务到 Quartz 调度器.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerJobInitializer implements ApplicationRunner {

	private final SysSchedulerJobRepository repository;

	private final SchedulerManager schedulerManager;

	@Override
	@NullMarked public void run(ApplicationArguments args) {
		List<SysSchedulerJob> jobs = this.repository.queryListByDsl(null);

		for (SysSchedulerJob job : jobs) {
			try {
				Class<?> jobClass = Class.forName(job.getJobClassName());
				if (!ClassUtils.isAssignable(Job.class, jobClass)) {
					log.warn("Skip invalid job class: {}", job.getJobClassName());
					continue;
				}

				boolean paused = !Status.ENABLED.getCode().equals(job.getStatus());
				this.schedulerManager.addJob(job.getJobName(), job.getJobClassName(), job.getCronExpression(),
						job.getJobData(), paused);
			}
			catch (ClassNotFoundException ex) {
				log.error("Job class not found: {}", job.getJobClassName(), ex);
			}
			catch (SchedulerException | JacksonException ex) {
				log.error("Failed to schedule job: {}", job.getJobName(), ex);
			}
		}

		log.info("Scheduler job initialization completed, loaded {} jobs", jobs.size());
	}

}
