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
 * 用户性别枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum UserGender implements BaseEnum<Integer> {

	/**
	 * 未知.
	 */
	UNKNOWN(0, "未知"),

	/**
	 * 男.
	 */
	MALE(1, "男"),

	/**
	 * 女.
	 */
	FEMALE(2, "女");

	/**
	 * 状态值.
	 */
	private final Integer code;

	/**
	 * 状态名称.
	 */
	private final String name;

	/**
	 * 根据 code 获取枚举实例.
	 * @param code 状态值
	 * @return UserGender 实例
	 */
	public static UserGender of(Integer code) {
		return BaseEnum.fromCode(UserGender.class, code);
	}

}
