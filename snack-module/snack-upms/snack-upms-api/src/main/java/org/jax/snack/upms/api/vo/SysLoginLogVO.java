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

import lombok.Getter;
import lombok.Setter;

/**
 * 登录日志 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysLoginLogVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * ID.
	 */
	private Long id;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 事件类型.
	 */
	private String action;

	/**
	 * 客户端IP地址.
	 */
	private String ipAddress;

	/**
	 * 浏览器User-Agent.
	 */
	private String userAgent;

	/**
	 * 会话ID.
	 */
	private String sessionId;

	/**
	 * 失败原因.
	 */
	private String failureReason;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

}
