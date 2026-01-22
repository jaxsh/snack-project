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
import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.annotation.TableLogic;
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
public class OAuth2User extends BaseEntity implements Serializable {

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
	 * 是否启用.
	 */
	private Boolean enabled;

	/**
	 * 是否锁定.
	 */
	private Boolean locked;

	/**
	 * 是否过期.
	 */
	private Boolean expired;

	/**
	 * 最后一次密码重置时间.
	 */
	private ZonedDateTime lastPasswordResetTime;

	/**
	 * 是否为初始密码.
	 */
	private Boolean initialPassword;

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
	 * 逻辑删除标识.
	 */
	@TableLogic
	private Integer deleted;

	/**
	 * 字段名常量, 继承父类字段.
	 */
	public static final class Fields extends BaseEntity.Fields {

	}

}
