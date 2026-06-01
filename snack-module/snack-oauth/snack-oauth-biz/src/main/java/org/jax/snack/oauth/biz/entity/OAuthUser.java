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

package org.jax.snack.oauth.biz.entity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * OAuth2 用户实体.
 * <p>
 * 仅存储认证凭证（用户名、密码、状态）。业务资料存储在 UPMS 模块中.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("oauth2_user")
public class OAuthUser extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 用户名.
	 */
	private String username;

	/**
	 * 密码.
	 */
	private String password;

	/**
	 * 手机号.
	 */
	private String mobile;

	/**
	 * 邮箱.
	 */
	private String email;

	/**
	 * 账户状态(0:禁用, 1:正常).
	 */
	private Integer enabled;

	/**
	 * 锁定状态(0:正常, 1:锁定).
	 */
	private Integer locked;

	/**
	 * 过期状态(0:正常, 1:过期).
	 */
	private Integer expired;

	/**
	 * 上次改密时间.
	 */
	private ZonedDateTime lastPasswordResetTime;

	/**
	 * 是否初始密码(0:否, 1:是).
	 */
	private Integer initialPassword;

	/**
	 * 锁定截止时间.
	 * <p>
	 * null 表示永久锁定.
	 */
	private ZonedDateTime lockUntil;

	/**
	 * 累计锁定次数.
	 */
	private Integer lockCount;

	/**
	 * 账号到期日.
	 * <p>
	 * null 表示永不到期.
	 */
	private LocalDate expireDate;

	public static final class Fields extends BaseEntity.Fields {

	}

}
