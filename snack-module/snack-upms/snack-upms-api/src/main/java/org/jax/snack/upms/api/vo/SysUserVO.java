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

import java.time.LocalDate;
import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysUserVO {

	/**
	 * 主键 ID.
	 */
	private Long id;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 真实姓名.
	 */
	private String realName;

	/**
	 * 昵称.
	 */
	private String nickname;

	/**
	 * 头像 URL.
	 */
	private String avatar;

	/**
	 * 性别 (0:未知, 1:男, 2:女).
	 */
	private Integer gender;

	/**
	 * 性别标签.
	 */
	private String genderLabel;

	/**
	 * 生日.
	 */
	private LocalDate birthday;

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
