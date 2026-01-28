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

package org.jax.snack.framework.message.event;

import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 消息日志事件.
 * <p>
 * 由业务系统监听此事件并保存发送日志.
 *
 * @author Jax Jiang
 */
@Getter
@Builder
public class MessageLogEvent {

	/**
	 * 模版编码.
	 */
	private String templateCode;

	/**
	 * 渠道类型.
	 */
	private String channelType;

	/**
	 * 渠道标识.
	 */
	private String channelId;

	/**
	 * 接收人.
	 */
	private String recipient;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 渲染后的内容.
	 */
	private String content;

	/**
	 * 占位符参数.
	 */
	private Map<String, Object> params;

	/**
	 * 状态(0:成功, 1:失败).
	 */
	private Integer status;

	/**
	 * 错误信息.
	 */
	private String errorMsg;

	/**
	 * 渠道原始响应.
	 */
	private Map<String, Object> channelResponse;

}
