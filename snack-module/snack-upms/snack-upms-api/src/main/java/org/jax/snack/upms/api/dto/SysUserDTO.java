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

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * 用户 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysUserDTO {

	/**
	 * 用户名.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 2, max = 64)
	private String username;

	/**
	 * 真实姓名.
	 */
	@Size(min = 1, max = 50)
	private String realName;

	/**
	 * 昵称.
	 */
	@Size(min = 1, max = 50)
	private String nickname;

	/**
	 * 头像 URL.
	 */
	private JsonNullable<@Size(min = 1, max = 255) String> avatar = JsonNullable.undefined();

	/**
	 * 性别(0:未知, 1:男, 2:女).
	 */
	@Min(0)
	@Max(2)
	private Integer gender;

	/**
	 * 生日.
	 */
	private JsonNullable<LocalDate> birthday = JsonNullable.undefined();

	/**
	 * 备注.
	 */
	private JsonNullable<@Size(max = 500) String> remark = JsonNullable.undefined();

	/**
	 * 关联角色编码集合.
	 */
	private JsonNullable<Set<String>> roleCodes = JsonNullable.undefined();

	/**
	 * 关联组织编码集合.
	 */
	private JsonNullable<Set<String>> orgCodes = JsonNullable.undefined();

	/**
	 * 账户状态(0:禁用, 1:启用).
	 */
	@Min(0)
	@Max(1)
	private Integer status;

	/**
	 * 手机号.
	 */
	@Pattern(regexp = "^1[3-9]\\d{9}$")
	private String mobile;

	/**
	 * 邮箱.
	 */
	@Email
	private String email;

	/**
	 * 新密码（仅用于重置密码操作）.
	 */
	@Size(min = 8, max = 255)
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,255}$")
	private String password;

	/**
	 * 账号到期日.
	 * <p>
	 * null 表示永不到期.
	 */
	private JsonNullable<LocalDate> expireDate = JsonNullable.undefined();

	/**
	 * 是否启用MFA(0:未启用, 1:已启用).
	 */
	@Min(0)
	@Max(1)
	private Integer mfaEnabled;

	/**
	 * MFA 验证码（开启时用于服务端校验，不存储）.
	 */
	@Pattern(regexp = "^[0-9]{6}$")
	private String mfaCode;

}
