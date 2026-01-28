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
import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 站内信表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_notification")
public class SysNotification extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 标题.
	 */
	private String title;

	/**
	 * 内容.
	 */
	private String content;

	/**
	 * 阅读标记(0:未读, 1:已读).
	 */
	private Integer readFlag;

	/**
	 * 阅读时间.
	 */
	private ZonedDateTime readTime;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
