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

package org.jax.snack.lowcode.biz.options;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 选项提供者接口.
 * <p>
 * 不同数据源 (static, dict, enum) 实现此接口.
 * </p>
 *
 * @author Jax Jiang
 */
public interface OptionsProvider {

	/**
	 * 获取选项列表.
	 * @param xOptions x-options 配置节点
	 * @return 选项列表
	 */
	List<OptionItem> getOptions(JsonNode xOptions);

	/**
	 * 是否支持该类型.
	 * @param type 数据源类型
	 * @return true 表示支持
	 */
	boolean supports(String type);

	/**
	 * 获取优先级 (数值越小越优先).
	 * @return 优先级
	 */
	default int getOrder() {
		return 100;
	}

}
