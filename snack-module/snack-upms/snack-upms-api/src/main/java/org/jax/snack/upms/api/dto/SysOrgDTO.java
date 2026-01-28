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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * 组织机构 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysOrgDTO {

	/**
	 * 机构名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 100)
	private String orgName;

	/**
	 * 简称/缩写.
	 */
	@Size(max = 20)
	@Pattern(regexp = "^[A-Za-z0-9]*$")
	private String shortName;

	/**
	 * 父节点编码 (根节点不传或传空).
	 */
	@Size(max = 64)
	private String parentCode;

	/**
	 * 省.
	 */
	@Size(max = 50)
	private String province;

	/**
	 * 市.
	 */
	@Size(max = 50)
	private String city;

	/**
	 * 区.
	 */
	@Size(max = 50)
	private String district;

	/**
	 * 详细地址.
	 */
	@Size(max = 200)
	private String address;

	/**
	 * 联系人.
	 */
	@Size(max = 50)
	private String contactName;

	/**
	 * 联系电话.
	 */
	@Size(max = 20)
	private String contactPhone;

	/**
	 * 排序.
	 */
	@Min(0)
	@Max(999)
	private Integer sortOrder;

	/**
	 * 状态(0:禁用, 1:启用).
	 */
	@NotNull(groups = Create.class)
	private Integer status;

}
