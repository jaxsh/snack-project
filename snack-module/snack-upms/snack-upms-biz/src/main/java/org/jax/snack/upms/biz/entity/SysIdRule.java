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
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;
import org.jax.snack.upms.api.dto.SysIdRuleSegmentDTO;
import org.jax.snack.upms.biz.enums.ResetCycle;

/**
 * ID 规则表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@TableName(autoResultMap = true)
public class SysIdRule extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 规则编码 (唯一标识).
	 */
	private String ruleCode;

	/**
	 * 规则名称.
	 */
	private String ruleName;

	/**
	 * 分段规则配置.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private List<SysIdRuleSegmentDTO> segments;

	/**
	 * 规则描述.
	 */
	private String description;

	/**
	 * 序列号重置周期.
	 */
	private ResetCycle resetCycle;

}
