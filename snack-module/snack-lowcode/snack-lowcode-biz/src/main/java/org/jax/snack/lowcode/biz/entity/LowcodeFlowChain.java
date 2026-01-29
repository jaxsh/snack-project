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
 * 流程链路定义实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
@FieldNameConstants
@TableName("lowcode_flow_chain")
public class LowcodeFlowChain extends BaseEntity {

	/**
	 * 关联业务流程ID.
	 */
	private Long flowId;

	/**
	 * 链路执行表达式(LiteFlow EL).
	 */
	private String chainEl;

	/**
	 * 链路可视化配置JSON.
	 */
	private String chainJson;

	/**
	 * 执行模式(SYNC:同步, ASYNC:异步).
	 */
	private String executeMode;

	/**
	 * 版本号.
	 */
	private Integer version;

	public static final class Fields extends BaseEntity.Fields {

	}

}
