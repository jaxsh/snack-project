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

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * ID 规则片段 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysIdRuleSegmentDTO {

	/**
	 * 片段类型.
	 */
	@NotBlank(groups = Create.class)
	private String segmentType;

	/**
	 * 片段配置.
	 */
	@NotNull(groups = Create.class)
	private Map<String, Object> config;

}
