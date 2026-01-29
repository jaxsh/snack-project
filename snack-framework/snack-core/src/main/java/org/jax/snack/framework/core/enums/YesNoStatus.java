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

package org.jax.snack.framework.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 通用是/否枚举.
 * <p>
 * 用于表示布尔语义的二元状态, 如"已读/未读"、"锁定/未锁定"、"初始密码/非初始密码"等.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum YesNoStatus implements BaseEnum<Integer> {

	/**
	 * 否.
	 */
	NO(0, "否"),

	/**
	 * 是.
	 */
	YES(1, "是");

	private final Integer code;

	private final String name;

	public static YesNoStatus of(Integer code) {
		return BaseEnum.fromCode(YesNoStatus.class, code);
	}

}
