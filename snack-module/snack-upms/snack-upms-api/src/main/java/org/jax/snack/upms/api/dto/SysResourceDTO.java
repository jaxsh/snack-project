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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 资源 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class SysResourceDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 父节点 ID.
	 */
	@NotNull(groups = Create.class)
	private Long parentId;

	/**
	 * 资源名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 50)
	private String name;

	/**
	 * 类型 (0:菜单, 1:按钮, 2:接口).
	 */
	@NotNull(groups = Create.class)
	private Integer type;

	/**
	 * 权限标识.
	 */
	@Size(max = 100)
	private String permission;

	/**
	 * 路由路径/接口URL.
	 */
	@Size(max = 255)
	private String path;

	/**
	 * 前端组件路径.
	 */
	@Size(max = 255)
	private String component;

	/**
	 * HTTP方法 (GET/POST).
	 */
	@Size(max = 10)
	private String method;

	/**
	 * 图标.
	 */
	@Size(max = 50)
	private String icon;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 是否可见.
	 */
	private Boolean visible;

	/**
	 * 状态 (1:启用, 0:禁用).
	 */
	private Integer status;

}
