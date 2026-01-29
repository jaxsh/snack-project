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

package org.jax.snack.lowcode.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.BaseEnum;

/**
 * 业务流触发类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum TriggerType implements BaseEnum<String> {

	/**
	 * 接口触发.
	 */
	API("API", "接口触发"),

	/**
	 * 定时触发.
	 */
	TIMER("TIMER", "定时触发"),

	/**
	 * 事件触发.
	 */
	EVENT("EVENT", "事件触发");

	private final String code;

	private final String name;

	public static TriggerType of(String code) {
		return BaseEnum.fromCode(TriggerType.class, code);
	}

}
