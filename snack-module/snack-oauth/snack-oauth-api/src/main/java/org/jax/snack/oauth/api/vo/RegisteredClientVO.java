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

package org.jax.snack.oauth.api.vo;

import java.time.Instant;
import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * OAuth2 客户端 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class RegisteredClientVO {

	/**
	 * 主键 ID.
	 */
	private String id;

	/**
	 * 客户端 ID.
	 */
	private String clientId;

	/**
	 * 客户端 ID 发行时间.
	 */
	private Instant clientIdIssuedAt;

	/**
	 * 客户端密钥过期时间.
	 */
	private Instant clientSecretExpiresAt;

	/**
	 * 客户端名称.
	 */
	private String clientName;

	/**
	 * 客户端认证方法.
	 */
	private String clientAuthenticationMethods;

	/**
	 * 授权类型.
	 */
	private String authorizationGrantTypes;

	/**
	 * 重定向 URI.
	 */
	private String redirectUris;

	/**
	 * 退出登录重定向 URI.
	 */
	private String postLogoutRedirectUris;

	/**
	 * 权限范围.
	 */
	private String scopes;

	/**
	 * 客户端设置.
	 */
	private String clientSettings;

	/**
	 * 令牌设置.
	 */
	private String tokenSettings;

	/**
	 * 创建时间.
	 */
	private ZonedDateTime createTime;

	/**
	 * 更新时间.
	 */
	private ZonedDateTime updateTime;

}
