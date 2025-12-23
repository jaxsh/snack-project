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

package org.jax.snack.upms.biz.entity;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

/**
 * ID 序列号表.
 * <p>
 * 轻量级实体，不继承 BaseEntity，无审计字段.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysIdSequence implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键 ID.
	 */
	@TableId(type = IdType.AUTO)
	private Long id;

	/**
	 * 规则 ID.
	 */
	private Long ruleId;

	/**
	 * 周期标识.
	 */
	private String cycleKey;

	/**
	 * 当前序列值.
	 */
	private Long currentValue;

}
