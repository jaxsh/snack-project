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

package org.jax.snack.lowcode.biz.liteflow.context;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 业务流程执行上下文.
 * <p>
 * 在 LiteFlow 组件之间传递数据.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class BusinessFlowContext {

	/**
	 * 流程编码.
	 */
	private String flowCode;

	/**
	 * 主 Schema 名称.
	 */
	private String mainSchema;

	/**
	 * 业务数据.
	 */
	private Map<String, Object> data;

	/**
	 * 主键 ID (UPDATE/DELETE 时使用).
	 */
	private Long id;

	/**
	 * 执行结果.
	 */
	private Object result;

	/**
	 * 执行是否成功.
	 */
	private boolean success = true;

	/**
	 * 错误消息.
	 */
	private String errorMessage;

	/**
	 * 组件配置 (stepId -> config).
	 */
	private Map<String, Map<String, Object>> stepConfigs = new HashMap<>();

	/**
	 * 中间变量 (组件间传递).
	 */
	private Map<String, Object> variables = new HashMap<>();

	/**
	 * 迭代器当前项 (ITERATOR 组件使用).
	 */
	private Object currentItem;

	/**
	 * 当前循环索引.
	 */
	private int loopIndex;

	/**
	 * 获取步骤配置.
	 * @param stepId 步骤 ID
	 * @return 配置 Map
	 */
	public Map<String, Object> getStepConfig(String stepId) {
		return this.stepConfigs.getOrDefault(stepId, new HashMap<>());
	}

	/**
	 * 设置步骤配置.
	 * @param stepId 步骤 ID
	 * @param config 配置
	 */
	public void setStepConfig(String stepId, Map<String, Object> config) {
		this.stepConfigs.put(stepId, config);
	}

	/**
	 * 获取变量值.
	 * @param name 变量名
	 * @return 变量值
	 */
	public Object getVariable(String name) {
		return this.variables.get(name);
	}

	/**
	 * 设置变量值.
	 * @param name 变量名
	 * @param value 变量值
	 */
	public void setVariable(String name, Object value) {
		this.variables.put(name, value);
	}

}
