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
 * 消息发送日志表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName(value = "sys_message_log", autoResultMap = true)
public class SysMessageLog extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 模版编码.
	 */
	private String templateCode;

	/**
	 * 渠道类型.
	 */
	private String channelType;

	/**
	 * 渠道标识.
	 */
	private String channelId;

	/**
	 * 接收人.
	 */
	private String recipient;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 渲染后内容.
	 */
	private String content;

	/**
	 * 占位符参数.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private Map<String, Object> params;

	/**
	 * 状态(0:成功, 1:失败).
	 */
	private Integer status;

	/**
	 * 失败原因.
	 */
	private String errorMsg;

	/**
	 * 渠道原始响应.
	 */
	@TableField(typeHandler = Jackson3TypeHandler.class)
	private Map<String, Object> channelResponse;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
