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

package org.jax.snack.upms.biz.entity;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 定时任务表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_scheduler_job")
public class SysSchedulerJob extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 任务名称 (唯一).
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
	 * 状态 (0:禁用/暂停, 1:启用/运行).
	 * @see org.jax.snack.upms.api.enums.Status
	 */
	private Integer status;

	/**
	 * 描述.
	 */
	private String description;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
