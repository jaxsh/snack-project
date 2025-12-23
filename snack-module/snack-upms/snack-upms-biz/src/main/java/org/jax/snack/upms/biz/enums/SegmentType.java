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

package org.jax.snack.upms.biz.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 片段类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum SegmentType {

	/**
	 * 固定字符串.
	 */
	FIXED("FIXED"),

	/**
	 * 日期时间.
	 */
	DATE("DATE"),

	/**
	 * 序列号.
	 */
	SEQUENCE("SEQUENCE"),

	/**
	 * 随机字符串.
	 */
	RANDOM("RANDOM"),

	/**
	 * 动态参数.
	 */
	ARG("ARG");

	@EnumValue
	@JsonValue
	private final String value;

}
