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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * 测试商品 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysTestProductVO {

	/**
	 * 主键ID.
	 */
	private Long id;

	/**
	 * 产品编码.
	 */
	private String productCode;

	/**
	 * 产品名称.
	 */
	private String productName;

	/**
	 * 价格.
	 */
	private BigDecimal price;

	/**
	 * 库存数量.
	 */
	private Integer stock;

	/**
	 * 累计销量.
	 */
	private Long totalSales;

	/**
	 * 状态.
	 */
	private Integer status;

	/**
	 * 状态标签.
	 */
	private String statusLabel;

	/**
	 * 优先级.
	 */
	private String priority;

	/**
	 * 优先级标签.
	 */
	private String priorityLabel;

	/**
	 * 商品分类.
	 */
	private String category;

	/**
	 * 商品分类标签.
	 */
	private String categoryLabel;

	/**
	 * 仓库.
	 */
	private Integer warehouse;

	/**
	 * 仓库标签.
	 */
	private String warehouseLabel;

	/**
	 * 标签.
	 */
	private String tag;

	/**
	 * 标签名称.
	 */
	private String tagLabel;

	/**
	 * 上架日期.
	 */
	private LocalDate launchDate;

	/**
	 * 创建时间.
	 */
	private LocalDateTime createdTime;

	/**
	 * 扩展信息.
	 */
	private String extraInfo;

	/**
	 * 产品描述.
	 */
	private String description;

}
