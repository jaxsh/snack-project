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
 * 消息模版视图对象.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysMessageTemplateVO implements Serializable {

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
	 * 模版名称.
	 */
	private String templateName;

	/**
	 * 模版类型(MAIL:邮件, SMS:短信, SITE:站内信).
	 */
	private String templateType;

	/**
	 * 模版类型标签.
	 */
	private String templateTypeLabel;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 模版内容.
	 */
	private String content;

	/**
	 * 渠道配置.
	 */
	private Map<String, Object> templateConfig;

	/**
	 * 状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 备注.
	 */
	private String remark;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
