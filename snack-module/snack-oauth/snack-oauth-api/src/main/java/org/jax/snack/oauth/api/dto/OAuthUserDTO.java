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

package org.jax.snack.oauth.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * OAuth2 用户传输对象.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class OAuthUserDTO {

	/**
	 * 用户名.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 2, max = 64)
	private String username;

	/**
	 * 密码.
	 */
	@Size(min = 8, max = 255)
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,255}$")
	private String password;

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
	 * 账户状态(0:禁用, 1:启用).
	 */
	private Integer enabled;

	/**
	 * 是否锁定(0:否, 1:是).
	 */
	private Integer locked;

	/**
	 * 是否初始密码(0:否, 1:是).
	 */
	private Integer initialPassword;

	/**
	 * 是否过期(0:否, 1:是).
	 */
	private Integer expired;

	/**
	 * 账号到期日.
	 * <p>
	 * null 表示永不到期.
	 */
	private LocalDate expireDate;

	/**
	 * 是否启用MFA(0:未启用, 1:已启用).
	 */
	@Min(0)
	@Max(1)
	private Integer mfaEnabled;

	/**
	 * MFA TOTP 密钥(Base32编码).
	 */
	private String mfaSecret;

}
