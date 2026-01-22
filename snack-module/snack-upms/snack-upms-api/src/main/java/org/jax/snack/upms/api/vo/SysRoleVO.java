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
import lombok.ToString;

/**
 * 角色 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class SysRoleVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键 ID.
	 */
	private Long id;

	/**
	 * 角色名称.
	 */
	private String roleName;

	/**
	 * 角色编码.
	 */
	private String roleCode;

	/**
	 * 角色描述.
	 */
	private String roleDesc;

	/**
	 * 状态 (1:启用, 0:禁用).
	 */
	private Integer status;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

}
