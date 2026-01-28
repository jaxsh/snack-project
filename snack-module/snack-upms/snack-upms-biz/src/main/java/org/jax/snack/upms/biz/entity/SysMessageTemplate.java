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
import java.util.Map;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 消息模版实体类.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName(value = "sys_message_template", autoResultMap = true)
public class SysMessageTemplate extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 模版编码.
	 */
	private String templateCode;

	/**
	 * 模版名称.
	 */
	private String templateName;

	/**
	 * 模板类型(MAIL:邮件, SMS:短信, SITE:站内信).
	 */
	private String templateType;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 模版内容.
	 */
	private String content;

	/**
	 * 渠道配置.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private Map<String, Object> templateConfig;

	/**
	 * 状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 备注.
	 */
	private String remark;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
