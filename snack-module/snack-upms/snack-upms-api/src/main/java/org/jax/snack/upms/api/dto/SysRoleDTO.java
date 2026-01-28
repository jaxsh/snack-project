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
import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 角色 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class SysRoleDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 角色名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 50)
	private String roleName;

	/**
	 * 角色编码.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 50)
	private String roleCode;

	/**
	 * 角色描述.
	 */
	@Size(max = 255)
	private String roleDesc;

	/**
	 * 状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 关联资源 ID 集合 (用于创建/更新角色时直接分配资源).
	 */
	private Set<Long> resourceIds;

}
