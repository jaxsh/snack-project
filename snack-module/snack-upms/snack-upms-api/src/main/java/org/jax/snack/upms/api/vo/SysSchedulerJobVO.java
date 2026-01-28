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

package org.jax.snack.upms.api.vo;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 定时任务 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysSchedulerJobVO {

	/**
	 * 主键 ID.
	 */
	private Long id;

	/**
	 * 任务名称.
	 */
	private String jobName;

	/**
	 * Job 执行类全限定名.
	 */
	private String jobClassName;

	/**
	 * Cron 表达式.
	 */
	private String cronExpression;

	/**
	 * 任务参数 (JSON).
	 */
	private String jobData;

	/**
	 * 状态(0:暂停, 1:运行).
	 */
	private Integer status;

	/**
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 描述.
	 */
	private String description;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
