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
 * 片段类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum SegmentType implements BaseEnum<String> {

	/**
	 * 固定字符串.
	 */
	FIXED("FIXED", "固定字符串"),

	/**
	 * 日期时间.
	 */
	DATE("DATE", "日期时间"),

	/**
	 * 序列号.
	 */
	SEQUENCE("SEQUENCE", "序列号"),

	/**
	 * 随机字符串.
	 */
	RANDOM("RANDOM", "随机字符串"),

	/**
	 * 动态参数.
	 */
	ARG("ARG", "动态参数");

	/**
	 * 状态值.
	 */
	private final String code;

	/**
	 * 状态名称.
	 */
	private final String name;

	/**
	 * 根据 code 获取枚举实例.
	 * @param code 状态值
	 * @return SegmentType 实例
	 */
	public static SegmentType of(String code) {
		return BaseEnum.fromCode(SegmentType.class, code);
	}

}
