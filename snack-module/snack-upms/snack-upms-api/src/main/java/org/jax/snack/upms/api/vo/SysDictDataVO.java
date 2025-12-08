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

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 字典数据 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysDictDataVO {

	/**
	 * 主键ID.
	 */
	private Long id;

	/**
	 * 字典类型.
	 */
	private String dictType;

	/**
	 * 字典标签.
	 */
	private String dictLabel;

	/**
	 * 字典值.
	 */
	private String dictValue;

	/**
	 * CSS类名.
	 */
	private String cssClass;

	/**
	 * 列表样式类名.
	 */
	private String listClass;

	/**
	 * 是否默认.
	 */
	private Boolean isDefault;

	/**
	 * 状态.
	 */
	private Integer status;

	/**
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 备注.
	 */
	private String remark;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
