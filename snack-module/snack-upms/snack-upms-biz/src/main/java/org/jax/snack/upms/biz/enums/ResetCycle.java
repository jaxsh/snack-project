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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 序列号重置周期枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum ResetCycle {

	/**
	 * 永不重置.
	 */
	NEVER("NEVER") {
		@Override
		public String computeCycleKey(LocalDate date) {
			return "GLOBAL";
		}
	},

	/**
	 * 每天重置.
	 */
	DAILY("DAILY") {
		@Override
		public String computeCycleKey(LocalDate date) {
			return date.format(DateTimeFormatter.BASIC_ISO_DATE);
		}
	},

	/**
	 * 每月重置.
	 */
	MONTHLY("MONTHLY") {
		@Override
		public String computeCycleKey(LocalDate date) {
			return date.format(DateTimeFormatter.ofPattern("yyyyMM"));
		}
	},

	/**
	 * 每年重置.
	 */
	YEARLY("YEARLY") {
		@Override
		public String computeCycleKey(LocalDate date) {
			return String.valueOf(date.getYear());
		}
	};

	@EnumValue
	@JsonValue
	private final String value;

	/**
	 * 根据日期计算周期标识.
	 * @param date 日期
	 * @return 周期标识
	 */
	public abstract String computeCycleKey(LocalDate date);

}
