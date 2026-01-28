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
 * 资源 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class SysResourceVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键 ID.
	 */
	private Long id;

	/**
	 * 父节点 ID.
	 */
	private Long parentId;

	/**
	 * 资源名称.
	 */
	private String name;

	/**
	 * 类型(0:菜单, 1:按钮, 2:接口).
	 */
	private Integer type;

	/**
	 * 类型标签.
	 */
	private String typeLabel;

	/**
	 * 权限标识.
	 */
	private String permission;

	/**
	 * 路由路径/接口URL.
	 */
	private String path;

	/**
	 * 前端组件路径.
	 */
	private String component;

	/**
	 * HTTP方法(GET, POST, etc).
	 */
	private String method;

	/**
	 * HTTP方法标签.
	 */
	private String methodLabel;

	/**
	 * 图标.
	 */
	private String icon;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 是否可见(0:隐藏, 1:显示).
	 */
	private Integer visible;

	/**
	 * 是否可见标签.
	 */
	private String visibleLabel;

	/**
	 * 状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

}
