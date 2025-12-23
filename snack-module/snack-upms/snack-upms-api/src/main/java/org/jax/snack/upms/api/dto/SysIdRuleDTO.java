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

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * ID 规则 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysIdRuleDTO {

	/**
	 * 规则编码.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 64)
	private String ruleCode;

	/**
	 * 规则名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 100)
	private String ruleName;

	/**
	 * 规则描述.
	 */
	@Size(max = 255)
	private String description;

	/**
	 * 序列号重置周期.
	 */
	@NotBlank(groups = Create.class)
	private String resetCycle;

	/**
	 * 片段列表.
	 */
	@NotEmpty(groups = Create.class)
	@Valid
	private List<SysIdRuleSegmentDTO> segments;

}
