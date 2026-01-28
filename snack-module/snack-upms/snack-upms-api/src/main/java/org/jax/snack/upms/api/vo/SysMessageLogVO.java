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

package org.jax.snack.upms.api.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * 消息发送日志视图对象.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysMessageLogVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * ID.
	 */
	private Long id;

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
	 * 内容.
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
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 错误信息.
	 */
	private String errorMsg;

	/**
	 * 渠道原始响应.
	 */
	private Map<String, Object> channelResponse;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
