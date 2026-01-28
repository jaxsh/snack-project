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

import java.io.File;
import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

/**
 * 消息发送请求.
 *
 * @author Jax Jiang
 */
@Getter
@Builder
public class MessageRequest {

	/**
	 * 模版编码.
	 */
	private String templateCode;

	/**
	 * 是否跳过模版查询.
	 */
	@Builder.Default
	private boolean skipTemplate = false;

	/**
	 * 渠道类型.
	 */
	private String channelType;

	/**
	 * 渠道标识.
	 */
	private String channelId;

	/**
	 * 接收人列表.
	 */
	private List<String> recipients;

	/**
	 * 抄送列表.
	 */
	private List<String> cc;

	/**
	 * 密送列表.
	 */
	private List<String> bcc;

	/**
	 * 附件列表.
	 */
	private List<File> attachments;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 内容.
	 */
	private String content;

	/**
	 * 占位符参数.
	 */
	private Map<String, Object> params;

}
