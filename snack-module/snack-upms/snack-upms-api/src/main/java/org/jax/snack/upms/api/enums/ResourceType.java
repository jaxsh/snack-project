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

package org.jax.snack.upms.api.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.BaseEnum;

/**
 * 资源类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum ResourceType implements BaseEnum<Integer> {

	/**
	 * 菜单.
	 */
	MENU(0, "菜单"),

	/**
	 * 按钮.
	 */
	BUTTON(1, "按钮"),

	/**
	 * 接口.
	 */
	API(2, "接口");

	private final Integer code;

	private final String name;

	public static ResourceType of(Integer code) {
		return BaseEnum.fromCode(ResourceType.class, code);
	}

}
