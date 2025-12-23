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

package org.jax.snack.upms.biz.generator;

import java.time.LocalDate;
import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.biz.entity.SysIdRule;

/**
 * ID 生成上下文.
 * <p>
 * 在生成过程中传递共享信息.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public class GeneratorContext {

	/**
	 * 规则信息.
	 */
	private final SysIdRule rule;

	/**
	 * 当前日期.
	 */
	private final LocalDate currentDate;

	/**
	 * 周期标识.
	 */
	private final String cycleKey;

	/**
	 * 动态参数.
	 */
	private final Map<String, String> args;

}
