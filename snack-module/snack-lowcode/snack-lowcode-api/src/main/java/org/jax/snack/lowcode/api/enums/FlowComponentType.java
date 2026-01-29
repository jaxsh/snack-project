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
 * 流程组件类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum FlowComponentType implements BaseEnum<String> {

	/**
	 * 增删改查.
	 */
	CRUD("CRUD", "增删改查"),

	/**
	 * 消息.
	 */
	MESSAGE("MESSAGE", "消息"),

	/**
	 * 网络请求.
	 */
	HTTP("HTTP", "网络请求"),

	/**
	 * 脚本.
	 */
	SCRIPT("SCRIPT", "脚本"),

	/**
	 * 条件.
	 */
	CONDITION("CONDITION", "条件"),

	/**
	 * 循环.
	 */
	ITERATOR("ITERATOR", "循环");

	private final String code;

	private final String name;

	public static FlowComponentType of(String code) {
		return BaseEnum.fromCode(FlowComponentType.class, code);
	}

}
