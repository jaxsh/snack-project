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

package org.jax.snack.upms.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 定时任务 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysSchedulerJobDTO {

	/**
	 * 任务名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 100)
	private String jobName;

	/**
	 * Job 执行类全限定名.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 255)
	private String jobClassName;

	/**
	 * Cron 表达式.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 100)
	private String cronExpression;

	/**
	 * 任务参数 (JSON).
	 */
	private String jobData;

	/**
	 * 状态 (0:禁用/暂停, 1:启用/运行).
	 * @see org.jax.snack.upms.api.enums.Status
	 */
	@NotNull(groups = Create.class)
	private Integer status;

	/**
	 * 描述.
	 */
	@Size(max = 500)
	private String description;

}
