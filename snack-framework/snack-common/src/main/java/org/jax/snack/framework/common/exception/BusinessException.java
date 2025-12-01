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

package org.jax.snack.framework.common.exception;

import java.io.Serial;

import lombok.Getter;

/**
 * 业务异常.
 * <p>
 * 适用于所有场景: Web、定时任务、消息队列等.
 *
 * @author Jax Jiang
 */
@Getter
public class BusinessException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	private final String errorCode;

	private final transient Object[] messageArgs;

	/**
	 * 创建业务异常.
	 * @param errorCode 错误码
	 * @param messageArgs 消息参数（用于国际化占位符）
	 */
	public BusinessException(String errorCode, Object... messageArgs) {
		super(errorCode);
		this.errorCode = errorCode;
		this.messageArgs = messageArgs;
	}

}
