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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

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
	@Size(min = 1, max = 64)
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
	@Size(min = 1, max = 255)
	private String avatar;

	/**
	 * 性别 (0:未知, 1:男, 2:女).
	 */
	@Min(0)
	@Max(2)
	private Integer gender;

	/**
	 * 生日.
	 */
	private LocalDate birthday;

	/**
	 * 备注.
	 */
	@Size(max = 500)
	private String remark;

	/**
	 * 关联角色编码集合.
	 */
	private Set<String> roleCodes;

	/**
	 * 关联组织编码集合.
	 */
	private Set<String> orgCodes;

}
