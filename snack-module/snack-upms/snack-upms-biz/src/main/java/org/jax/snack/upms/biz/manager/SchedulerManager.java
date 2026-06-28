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

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

/**
 * Quartz 调度器管理器.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerManager {

	private static final String DEFAULT_GROUP = "DEFAULT";

	private final Scheduler scheduler;

	private final JsonMapper jsonMapper;

	/**
	 * 添加定时任务.
	 * @param jobName 任务名称
	 * @param jobClassName Job 执行类
	 * @param cronExpression Cron 表达式
	 * @param jobData 任务参数 (JSON)
	 * @param paused 是否暂停
	 */
	public void addJob(String jobName, String jobClassName, String cronExpression, String jobData, boolean paused)
			throws SchedulerException, ClassNotFoundException, JacksonException {
		JobDetail jobDetail = buildJobDetail(jobName, jobClassName, jobData);

		CronTrigger trigger = TriggerBuilder.newTrigger()
			.withIdentity(jobName, DEFAULT_GROUP)
			.forJob(jobDetail)
			.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
			.build();

		this.scheduler.scheduleJob(jobDetail, trigger);

		if (paused) {
			this.scheduler.pauseTrigger(trigger.getKey());
		}

		log.info("Scheduled job: {} with cron: {}", jobName, cronExpression);
	}

	/**
	 * 更新任务的 Cron 表达式.
	 * @param jobName 任务名称
	 * @param cronExpression 新s的 Cron 表达式
	 */
	public void updateCron(String jobName, String cronExpression) throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, DEFAULT_GROUP);
		CronTrigger trigger = (CronTrigger) this.scheduler.getTrigger(triggerKey);
		if (trigger == null) {
			log.warn("Trigger not found for job: {}", jobName);
			return;
		}

		CronTrigger newTrigger = trigger.getTriggerBuilder()
			.withIdentity(triggerKey)
			.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
			.build();

		Trigger.TriggerState oldState = this.scheduler.getTriggerState(triggerKey);
		this.scheduler.rescheduleJob(triggerKey, newTrigger);

		if (Trigger.TriggerState.PAUSED.equals(oldState)) {
			this.scheduler.pauseTrigger(triggerKey);
		}

		log.info("Updated cron for job: {} to: {}, state: {}", jobName, cronExpression, oldState);
	}

	/**
	 * 更新任务的 JobData.
	 * @param jobName 任务名称
	 * @param jobClassName Job 执行类
	 * @param jobData 新的任务参数 (JSON)
	 */
	public void updateJobData(String jobName, String jobClassName, String jobData)
			throws SchedulerException, ClassNotFoundException, JacksonException {
		JobDetail jobDetail = buildJobDetail(jobName, jobClassName, jobData);
		this.scheduler.addJob(jobDetail, true, true);
		log.info("Updated job data for: {}", jobName);
	}

	/**
	 * 暂停任务.
	 * @param jobName 任务名称
	 */
	public void pauseJob(String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_GROUP);
		this.scheduler.pauseJob(jobKey);
		log.info("Paused job: {}", jobName);
	}

	/**
	 * 恢复任务.
	 * @param jobName 任务名称
	 */
	public void resumeJob(String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_GROUP);
		this.scheduler.resumeJob(jobKey);
		log.info("Resumed job: {}", jobName);
	}

	/**
	 * 删除任务.
	 * @param jobName 任务名称
	 */
	public void deleteJob(String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_GROUP);
		this.scheduler.deleteJob(jobKey);
		log.info("Deleted job: {}", jobName);
	}

	/**
	 * 立即触发任务.
	 * @param jobName 任务名称
	 */
	public void triggerJob(String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_GROUP);
		this.scheduler.triggerJob(jobKey);
		log.info("Triggered job: {}", jobName);
	}

	/**
	 * 检查任务是否存在.
	 * @param jobName 任务名称
	 * @return 是否存在
	 */
	public boolean checkJobExists(String jobName) throws SchedulerException {
		JobKey jobKey = JobKey.jobKey(jobName, DEFAULT_GROUP);
		return this.scheduler.checkExists(jobKey);
	}

	/**
	 * 获取任务状态.
	 * @param jobName 任务名称
	 * @return 触发器状态
	 */
	public Trigger.TriggerState getJobState(String jobName) throws SchedulerException {
		TriggerKey triggerKey = TriggerKey.triggerKey(jobName, DEFAULT_GROUP);
		return this.scheduler.getTriggerState(triggerKey);
	}

	/**
	 * 构建 JobDetail 对象.
	 * @param jobName 任务名称
	 * @param jobClassName 执行类名
	 * @param jobData 任务数据 (JSON)
	 * @return JobDetail
	 * @throws ClassNotFoundException 类未找到异常
	 * @throws JacksonException JSON 处理异常
	 */
	private JobDetail buildJobDetail(String jobName, String jobClassName, String jobData)
			throws ClassNotFoundException, JacksonException {
		Class<?> clazz = Class.forName(jobClassName);
		Class<? extends Job> jobClass = clazz.asSubclass(Job.class);
		JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(jobName, DEFAULT_GROUP).storeDurably().build();

		if (!ObjectUtils.isEmpty(jobData)) {
			Map<String, ?> jobDataMap = this.jsonMapper.readValue(jobData, new TypeReference<>() {
			});
			jobDetail.getJobDataMap().putAll(jobDataMap);
		}
		return jobDetail;
	}

}
