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
 * 页面类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum PageType implements BaseEnum<String> {

	/**
	 * 创建.
	 */
	CREATE("create", "新增"),

	/**
	 * 编辑.
	 */
	EDIT("edit", "编辑"),

	/**
	 * 列表.
	 */
	LIST("list", "列表"),

	/**
	 * 详情.
	 */
	DETAIL("detail", "详情");

	private final String code;

	private final String name;

	public static PageType of(String code) {
		return BaseEnum.fromCode(PageType.class, code);
	}

}
