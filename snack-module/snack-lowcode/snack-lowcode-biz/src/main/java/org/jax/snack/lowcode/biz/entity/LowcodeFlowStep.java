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
 * 流程步骤配置实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
@FieldNameConstants
@TableName("lowcode_flow_step")
public class LowcodeFlowStep extends BaseEntity {

	/**
	 * 关联流程链路.
	 */
	private Long chainId;

	/**
	 * 步骤ID.
	 */
	private String stepId;

	/**
	 * 组件类型(CRUD:增删改查, MESSAGE:消息, HTTP:网络请求, SCRIPT:脚本, CONDITION:条件, ITERATOR:循环).
	 */
	private String componentType;

	/**
	 * 步骤名称.
	 */
	private String stepName;

	/**
	 * 组件配置JSON.
	 */
	private String configJson;

	/**
	 * 执行模式(SYNC:同步, ASYNC:异步).
	 */
	private String executeMode;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 是否启用(0:禁用, 1:启用).
	 */
	private Integer enabled;

	public static final class Fields extends BaseEntity.Fields {

	}

}
