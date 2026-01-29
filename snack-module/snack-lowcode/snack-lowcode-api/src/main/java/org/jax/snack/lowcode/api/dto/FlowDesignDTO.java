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

package org.jax.snack.lowcode.api.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 流程设计 DTO.
 * <p>
 * 用于前端可视化设计器保存流程定义.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class FlowDesignDTO {

	/**
	 * 流程编码.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 100)
	@Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "必须为下划线命名格式")
	private String flowCode;

	/**
	 * 流程名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 200)
	private String flowName;

	/**
	 * 描述.
	 */
	@Size(max = 500)
	private String description;

	/**
	 * 主 Schema 名称.
	 */
	@Size(max = 100)
	private String mainSchema;

	/**
	 * 触发类型 (API/SCHEDULE/EVENT).
	 */
	@Size(max = 20)
	private String triggerType;

	/**
	 * 节点列表.
	 */
	@NotEmpty(groups = Create.class)
	@Valid
	private List<FlowNodeDTO> nodes;

	/**
	 * 边列表 (连线).
	 */
	@Valid
	private List<FlowEdgeDTO> edges;

	/**
	 * 原始设计器 JSON (用于回显).
	 */
	private String designJson;

	/**
	 * 流程节点 DTO.
	 */
	@Getter
	@Setter
	public static class FlowNodeDTO {

		/**
		 * 节点 ID.
		 */
		@NotBlank
		@Size(max = 64)
		private String id;

		/**
		 * 组件类型 (CRUD/MESSAGE/HTTP/ITERATOR/CONDITION).
		 */
		@NotBlank
		@Size(max = 32)
		private String type;

		/**
		 * 节点名称.
		 */
		@Size(max = 128)
		private String name;

		/**
		 * 节点位置.
		 */
		@Valid
		private Position position;

		/**
		 * 组件配置.
		 */
		private Map<String, Object> config;

		/**
		 * 执行模式 (SYNC/ASYNC).
		 */
		@Size(max = 10)
		private String executeMode;

	}

	/**
	 * 流程边 DTO (连线).
	 */
	@Getter
	@Setter
	public static class FlowEdgeDTO {

		/**
		 * 源节点 ID.
		 */
		@NotBlank
		@Size(max = 64)
		private String source;

		/**
		 * 目标节点 ID.
		 */
		@NotBlank
		@Size(max = 64)
		private String target;

		/**
		 * 关系类型 (用于特殊连线, 如 ITERATOR 的 DO).
		 */
		@Size(max = 32)
		private String relation;

	}

	/**
	 * 节点位置.
	 */
	@Getter
	@Setter
	public static class Position {

		private Integer x;

		private Integer y;

	}

}
