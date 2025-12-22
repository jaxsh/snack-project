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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 低代码 Schema DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class LowcodeSchemaDTO {

	/**
	 * 实体名称 (下划线命名, e.g. sys_product).
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 128)
	@Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "必须为下划线命名格式")
	private String schemaName;

	/**
	 * API 路径 (kebab-case, e.g. sys-products).
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 128)
	@Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "必须为 kebab-case 格式")
	private String resourcePath;

	/**
	 * 表名 (下划线命名, e.g. lc_sys_product).
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 128)
	@Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "必须为下划线命名格式")
	private String tableName;

	/**
	 * 显示名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 128)
	private String label;

	/**
	 * 描述.
	 */
	@Size(max = 500)
	private String description;

	/**
	 * 字段列表.
	 */
	@NotEmpty(groups = Create.class)
	@Valid
	private List<FieldDTO> fields;

	/**
	 * 联合索引列表.
	 */
	@Valid
	private List<IndexDTO> indexes;

	/**
	 * 字段定义 DTO.
	 */
	@Getter
	@Setter
	public static class FieldDTO {

		/**
		 * 字段唯一标识, 用于追踪字段变更.
		 * <p>
		 * 新增字段时为 null, 修改时由后端自动填充.
		 * </p>
		 */
		private String fieldId;

		/**
		 * 字段名 (驼峰命名).
		 */
		@NotBlank
		@Size(max = 64)
		private String fieldName;

		/**
		 * 数据库列名 (下划线命名).
		 */
		@NotBlank
		@Size(max = 64)
		private String dbColumn;

		/**
		 * 字段标题.
		 */
		@NotBlank
		@Size(max = 64)
		private String title;

		/**
		 * 逻辑数据类型 (e.g. string, int, decimal).
		 * <p>
		 * 对应 LowcodeDataType.logicType
		 * </p>
		 */
		@NotBlank
		@Size(max = 32)
		private String logicType;

		/**
		 * 长度.
		 */
		private Integer length;

		/**
		 * 精度.
		 */
		private Integer scale;

		/**
		 * 是否允许为空.
		 */
		private Boolean nullable;

		/**
		 * 默认值.
		 */
		@Size(max = 255)
		private String defaultValue;

		/**
		 * 字段注释.
		 */
		@Size(max = 255)
		private String comment;

		/**
		 * 是否创建索引.
		 */
		private Boolean index;

		/**
		 * 是否唯一约束.
		 */
		private Boolean unique;

	}

	/**
	 * 索引定义 DTO.
	 */
	@Getter
	@Setter
	public static class IndexDTO {

		/**
		 * 索引名称.
		 * <p>
		 * 可选, 不填则自动生成 (格式: idx_{columns[0]}_{columns[1]}_...).
		 * </p>
		 */
		@Size(max = 64)
		private String indexName;

		/**
		 * 索引列列表 (按顺序).
		 */
		@NotEmpty
		private List<String> columns;

		/**
		 * 是否唯一索引.
		 */
		private Boolean unique;

	}

}
