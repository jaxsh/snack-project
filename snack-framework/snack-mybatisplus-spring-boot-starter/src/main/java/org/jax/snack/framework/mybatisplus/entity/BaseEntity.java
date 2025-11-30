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

package org.jax.snack.framework.mybatisplus.entity;

import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * 通用实体基类.
 * <p>
 * 提供主键和审计字段 (创建时间、创建人、更新时间、更新人).
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public abstract class BaseEntity {

	/**
	 * 主键ID.
	 * <p>
	 * 使用数据库自增策略.
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 创建时间.
	 * <p>
	 * 插入时自动填充.
	 */
	@TableField(fill = FieldFill.INSERT)
	private ZonedDateTime createTime;

	/**
	 * 创建人.
	 * <p>
	 * 插入时自动填充.
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 更新时间.
	 * <p>
	 * 插入和更新时自动填充.
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private ZonedDateTime updateTime;

	/**
	 * 更新人.
	 * <p>
	 * 插入和更新时自动填充.
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

}
