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
 * 消息模板类型枚举.
 *
 * @author Jax Jiang
 */
@Getter
@RequiredArgsConstructor
public enum MessageTemplateType implements BaseEnum<String> {

	/**
	 * 邮件.
	 */
	MAIL("MAIL", "邮件"),

	/**
	 * 短信.
	 */
	SMS("SMS", "短信"),

	/**
	 * 站内信.
	 */
	SITE("SITE", "站内信");

	private final String code;

	private final String name;

	public static MessageTemplateType of(String code) {
		return BaseEnum.fromCode(MessageTemplateType.class, code);
	}

}
