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

package org.jax.snack.framework.message.core;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 消息发送结果.
 *
 * @author Jax Jiang
 */
@Getter
@Builder
public class MessageResult {

	/**
	 * 是否成功.
	 */
	private boolean success;

	/**
	 * 错误信息.
	 */
	private String errorMsg;

	/**
	 * 渠道原始响应.
	 */
	private Map<String, Object> rawResponse;

	/**
	 * 创建成功结果.
	 * @return 成功结果
	 */
	public static MessageResult ofSuccess() {
		return MessageResult.builder().success(true).build();
	}

	/**
	 * 创建成功结果 (带原始响应).
	 * @param rawResponse 原始响应
	 * @return 成功结果
	 */
	public static MessageResult ofSuccess(Map<String, Object> rawResponse) {
		return MessageResult.builder().success(true).rawResponse(rawResponse).build();
	}

	/**
	 * 创建失败结果.
	 * @param errorMsg 错误信息
	 * @return 失败结果
	 */
	public static MessageResult ofFail(String errorMsg) {
		return MessageResult.builder().success(false).errorMsg(errorMsg).build();
	}

}
