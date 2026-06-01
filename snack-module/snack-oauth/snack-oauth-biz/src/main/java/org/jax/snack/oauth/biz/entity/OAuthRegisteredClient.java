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
import java.time.Instant;
import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * OAuth2 客户端注册实体.
 * <p>
 * 映射 oauth2_registered_client 表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("oauth2_registered_client")
public class OAuthRegisteredClient implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键ID.
	 */
	@TableId(type = IdType.ASSIGN_UUID)
	private String id;

	/**
	 * 客户端ID.
	 */
	private String clientId;

	/**
	 * 客户端ID发行时间.
	 */
	private Instant clientIdIssuedAt;

	/**
	 * 客户端密钥.
	 */
	private String clientSecret;

	/**
	 * 客户端密钥过期时间.
	 */
	private Instant clientSecretExpiresAt;

	/**
	 * 客户端名称.
	 */
	private String clientName;

	/**
	 * 客户端认证方法(逗号分隔).
	 */
	private String clientAuthenticationMethods;

	/**
	 * 授权类型(逗号分隔).
	 */
	private String authorizationGrantTypes;

	/**
	 * 重定向URI(逗号分隔).
	 */
	private String redirectUris;

	/**
	 * 退出登录重定向URI(逗号分隔).
	 */
	private String postLogoutRedirectUris;

	/**
	 * 权限范围(逗号分隔).
	 */
	private String scopes;

	/**
	 * 客户端设置 (JSON).
	 */
	private String clientSettings;

	/**
	 * 令牌设置 (JSON).
	 */
	private String tokenSettings;

	/**
	 * 创建时间.
	 */
	@TableField(fill = FieldFill.INSERT)
	private ZonedDateTime createTime;

	/**
	 * 创建人.
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 更新时间.
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private ZonedDateTime updateTime;

	/**
	 * 更新人.
	 */
	@TableField(fill = FieldFill.INSERT_UPDATE)
	private String updateBy;

}
