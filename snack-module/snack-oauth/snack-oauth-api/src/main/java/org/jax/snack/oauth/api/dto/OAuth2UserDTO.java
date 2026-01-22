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

import jakarta.validation.constraints.Email;
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
public class OAuth2UserDTO {

	/**
	 * 用户名.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 2, max = 64)
	private String username;

	/**
	 * 密码.
	 */
	@Size(min = 6, max = 255)
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

}
