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

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * OAuth2 授权实体.
 * <p>
 * 映射 oauth2_authorization 表，ID 由 Spring Security 框架赋值.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@FieldNameConstants
@TableName("oauth2_authorization")
public class OAuthAuthorization implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键 ID.
	 */
	@TableId(type = IdType.INPUT)
	private String id;

	/**
	 * 注册客户端 ID.
	 */
	private String registeredClientId;

	/**
	 * 用户主体名称.
	 */
	private String principalName;

	/**
	 * 授权类型.
	 */
	private String authorizationGrantType;

	/**
	 * 已授权范围(逗号分隔).
	 */
	private String authorizedScopes;

	/**
	 * 属性(JSON).
	 */
	private String attributes;

	/**
	 * 状态.
	 */
	private String state;

	/**
	 * 授权码值.
	 */
	private String authorizationCodeValue;

	/**
	 * 授权码发行时间.
	 */
	private Instant authorizationCodeIssuedAt;

	/**
	 * 授权码过期时间.
	 */
	private Instant authorizationCodeExpiresAt;

	/**
	 * 授权码元数据(JSON).
	 */
	private String authorizationCodeMetadata;

	/**
	 * 访问令牌值.
	 */
	private String accessTokenValue;

	/**
	 * 访问令牌发行时间.
	 */
	private Instant accessTokenIssuedAt;

	/**
	 * 访问令牌过期时间.
	 */
	private Instant accessTokenExpiresAt;

	/**
	 * 访问令牌元数据(JSON).
	 */
	private String accessTokenMetadata;

	/**
	 * 访问令牌类型.
	 */
	private String accessTokenType;

	/**
	 * 访问令牌范围(逗号分隔).
	 */
	private String accessTokenScopes;

	/**
	 * OIDC ID Token 值.
	 */
	private String oidcIdTokenValue;

	/**
	 * OIDC ID Token 发行时间.
	 */
	private Instant oidcIdTokenIssuedAt;

	/**
	 * OIDC ID Token 过期时间.
	 */
	private Instant oidcIdTokenExpiresAt;

	/**
	 * OIDC ID Token 元数据(JSON).
	 */
	private String oidcIdTokenMetadata;

	/**
	 * OIDC ID Token 声明(JSON).
	 */
	private String oidcIdTokenClaims;

	/**
	 * 刷新令牌值.
	 */
	private String refreshTokenValue;

	/**
	 * 刷新令牌发行时间.
	 */
	private Instant refreshTokenIssuedAt;

	/**
	 * 刷新令牌过期时间.
	 */
	private Instant refreshTokenExpiresAt;

	/**
	 * 刷新令牌元数据(JSON).
	 */
	private String refreshTokenMetadata;

	/**
	 * 用户码值.
	 */
	private String userCodeValue;

	/**
	 * 用户码发行时间.
	 */
	private Instant userCodeIssuedAt;

	/**
	 * 用户码过期时间.
	 */
	private Instant userCodeExpiresAt;

	/**
	 * 用户码元数据(JSON).
	 */
	private String userCodeMetadata;

	/**
	 * 设备码值.
	 */
	private String deviceCodeValue;

	/**
	 * 设备码发行时间.
	 */
	private Instant deviceCodeIssuedAt;

	/**
	 * 设备码过期时间.
	 */
	private Instant deviceCodeExpiresAt;

	/**
	 * 设备码元数据(JSON).
	 */
	private String deviceCodeMetadata;

}
