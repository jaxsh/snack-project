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

import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 字典类型表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysDictType extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 字典名称.
	 */
	private String dictName;

	/**
	 * 字典类型 (业务上唯一).
	 */
	private String dictType;

	/**
	 * 状态.
	 */
	private Integer status;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 备注.
	 */
	private String remark;

}
