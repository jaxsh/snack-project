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

package org.jax.snack.lowcode.biz.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 业务流程定义实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
@FieldNameConstants
@TableName("lowcode_business_flow")
public class LowcodeBusinessFlow extends BaseEntity {

	/**
	 * 业务流程编码.
	 */
	private String flowCode;

	/**
	 * 业务流程名称.
	 */
	private String flowName;

	/**
	 * 描述.
	 */
	private String description;

	/**
	 * 主模型名称.
	 */
	private String mainSchema;

	/**
	 * 触发类型(API:接口触发, SCHEDULE:定时触发, EVENT:事件触发).
	 */
	private String triggerType;

	/**
	 * 是否启用(0:禁用, 1:启用).
	 */
	private Integer enabled;

	/**
	 * 版本号.
	 */
	private Integer version;

	public static final class Fields extends BaseEntity.Fields {

	}

}
