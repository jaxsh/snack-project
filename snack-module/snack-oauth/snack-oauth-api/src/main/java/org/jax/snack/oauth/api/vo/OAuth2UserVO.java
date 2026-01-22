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
public class OAuth2UserVO {

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
	 * 是否启用.
	 */
	private Boolean enabled;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

	/**
	 * 是否为初始密码.
	 */
	private Boolean initialPassword;

}
