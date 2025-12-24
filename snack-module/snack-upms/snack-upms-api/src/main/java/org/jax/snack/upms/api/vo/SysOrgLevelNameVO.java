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
 * 组织层级名称 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysOrgLevelNameVO {

	/**
	 * 主键ID.
	 */
	private Long id;

	/**
	 * 关联根节点ID.
	 */
	private Long rootId;

	/**
	 * 层级编号.
	 */
	private Integer level;

	/**
	 * 层级名称.
	 */
	private String levelName;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
