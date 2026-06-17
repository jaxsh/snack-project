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

package org.jax.snack.oauth.api.vo;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * OAuth2 用户 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class OAuthUserVO {

	/**
	 * 主键ID.
	 */
	private Long id;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 手机号.
	 */
	private String mobile;

	/**
	 * 邮箱.
	 */
	private String email;

	/**
	 * 账户状态(0:禁用, 1:正常).
	 */
	private Integer enabled;

	/**
	 * 账户状态标签.
	 */
	private String enabledLabel;

	/**
	 * 锁定状态(0:正常, 1:锁定).
	 */
	private Integer locked;

	/**
	 * 锁定状态标签.
	 */
	private String lockedLabel;

	/**
	 * 过期状态(0:正常, 1:过期).
	 */
	private Integer expired;

	/**
	 * 过期状态标签.
	 */
	private String expiredLabel;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

	/**
	 * 是否为初始密码(0:否, 1:是).
	 */
	private Integer initialPassword;

	/**
	 * 是否为初始密码标签.
	 */
	private String initialPasswordLabel;

	/**
	 * 是否启用 MFA(0:未启用, 1:已启用).
	 */
	private Integer mfaEnabled;

}
