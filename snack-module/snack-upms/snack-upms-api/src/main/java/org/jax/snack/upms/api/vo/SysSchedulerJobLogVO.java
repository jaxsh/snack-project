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
 * 定时任务执行日志 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysSchedulerJobLogVO {

	/**
	 * 主键 ID.
	 */
	private Long id;

	/**
	 * 关联任务 ID.
	 */
	private Long jobId;

	/**
	 * 任务名称.
	 */
	private String jobName;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

	/**
	 * 执行时长 (ms).
	 */
	private Long duration;

	/**
	 * 执行状态 (0:失败, 1:成功).
	 */
	private Integer status;

	/**
	 * 错误信息.
	 */
	private String errorMessage;

}
