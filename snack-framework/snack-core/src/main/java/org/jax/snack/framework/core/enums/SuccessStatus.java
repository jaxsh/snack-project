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
 * 通用成功/失败枚举.
 * <p>
 * 用于表示操作结果状态, 如任务执行结果、消息发送结果等.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum SuccessStatus implements BaseEnum<Integer> {

	/**
	 * 失败.
	 */
	FAIL(0, "失败"),

	/**
	 * 成功.
	 */
	SUCCESS(1, "成功");

	private final Integer code;

	private final String name;

	public static SuccessStatus of(Integer code) {
		return BaseEnum.fromCode(SuccessStatus.class, code);
	}

}
