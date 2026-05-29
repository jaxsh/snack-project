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
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 用户表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("sys_user")
public class SysUser extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 真实姓名.
	 */
	private String realName;

	/**
	 * 昵称.
	 */
	private String nickname;

	/**
	 * 头像URL.
	 */
	private String avatar;

	/**
	 * 性别(0:未知, 1:男, 2:女).
	 */
	private Integer gender;

	/**
	 * 生日.
	 */
	private LocalDate birthday;

	/**
	 * 备注.
	 */
	private String remark;

	/**
	 * 账户状态(0:禁用, 1:启用).
	 */
	private Integer status;

	/**
	 * 手机号.
	 */
	private String mobile;

	/**
	 * 邮箱.
	 */
	private String email;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
