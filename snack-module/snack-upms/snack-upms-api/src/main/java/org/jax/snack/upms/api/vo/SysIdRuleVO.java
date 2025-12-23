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
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * ID 规则 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysIdRuleVO {

	/**
	 * 规则 ID.
	 */
	private Long id;

	/**
	 * 规则编码.
	 */
	private String ruleCode;

	/**
	 * 规则名称.
	 */
	private String ruleName;

	/**
	 * 规则描述.
	 */
	private String description;

	/**
	 * 序列号重置周期.
	 */
	private String resetCycle;

	/**
	 * 片段列表.
	 */
	private List<SysIdRuleSegmentVO> segments;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
