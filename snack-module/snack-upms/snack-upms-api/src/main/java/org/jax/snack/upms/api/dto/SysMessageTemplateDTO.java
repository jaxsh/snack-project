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

package org.jax.snack.upms.api.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 消息模版传输对象.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysMessageTemplateDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * ID (Update only).
	 */
	private Long id;

	/**
	 * 模版编码.
	 */
	@NotBlank
	private String templateCode;

	/**
	 * 模版名称.
	 */
	@NotBlank
	private String templateName;

	/**
	 * 模版类型(MAIL:邮件, SMS:短信, SITE:站内信).
	 */
	@NotBlank
	private String templateType;

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
	 * 备注.
	 */
	private String remark;

}
