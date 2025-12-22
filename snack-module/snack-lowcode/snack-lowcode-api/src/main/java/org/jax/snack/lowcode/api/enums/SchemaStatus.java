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
 * Schema 状态枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum SchemaStatus implements BaseEnum<Integer> {

	/**
	 * 草稿.
	 */
	DRAFT(0, "草稿"),

	/**
	 * 已发布.
	 */
	PUBLISHED(1, "已发布");

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
	 * @return SchemaStatus 实例
	 */
	public static SchemaStatus of(Integer code) {
		return BaseEnum.fromCode(SchemaStatus.class, code);
	}

}
