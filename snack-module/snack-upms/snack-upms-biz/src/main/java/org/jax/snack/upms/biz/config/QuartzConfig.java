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

package org.jax.snack.upms.biz.config;

import org.jax.snack.upms.biz.manager.JobLogListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz 调度器配置.
 *
 * @author Jax Jiang
 */
@Configuration
@ConditionalOnClass(Scheduler.class)
public class QuartzConfig {

	/**
	 * 注册任务执行监听器.
	 * @param scheduler Quartz 调度器
	 * @param jobLogListener 任务日志监听器
	 */
	public QuartzConfig(Scheduler scheduler, JobLogListener jobLogListener) throws SchedulerException {
		scheduler.getListenerManager().addJobListener(jobLogListener);
	}

}
