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

package org.jax.snack.upms.biz.entity;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 资源实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_resource")
public class SysResource extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 父节点ID.
	 */
	private Long parentId;

	/**
	 * 资源名称.
	 */
	private String name;

	/**
	 * 资源类型(0:菜单, 1:按钮, 2:接口).
	 */
	private Integer type;

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
	 * HTTP方法(GET:GET, POST:POST).
	 */
	private String method;

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
	 * 状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
