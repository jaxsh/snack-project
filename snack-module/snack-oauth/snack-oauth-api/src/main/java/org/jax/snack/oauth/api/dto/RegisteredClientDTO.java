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

package org.jax.snack.oauth.api.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;

/**
 * OAuth2 客户端 DTO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString
public class RegisteredClientDTO {

	/**
	 * 客户端 ID.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 100)
	private String clientId;

	/**
	 * 客户端 ID 发行时间.
	 */
	private Instant clientIdIssuedAt;

	/**
	 * 客户端密钥.
	 */
	@Size(max = 200)
	private String clientSecret;

	/**
	 * 客户端密钥过期时间.
	 */
	private Instant clientSecretExpiresAt;

	/**
	 * 客户端名称.
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 200)
	@Size(min = 1, max = 200)
	private String clientName;

	/**
	 * 客户端认证方法 (逗号分隔).
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 1000)
	private String clientAuthenticationMethods;

	/**
	 * 授权类型 (逗号分隔).
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 1000)
	private String authorizationGrantTypes;

	/**
	 * 重定向 URI (逗号分隔).
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 1000)
	private String redirectUris;

	/**
	 * 退出登录重定向 URI (逗号分隔).
	 */
	@Size(min = 1, max = 1000)
	private String postLogoutRedirectUris;

	/**
	 * 权限范围 (逗号分隔).
	 */
	@NotBlank(groups = Create.class)
	@Size(min = 1, max = 1000)
	private String scopes;

	/**
	 * 客户端设置 (JSON).
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 2000)
	private String clientSettings;

	/**
	 * 令牌设置 (JSON).
	 */
	@NotBlank(groups = Create.class)
	@Size(max = 2000)
	private String tokenSettings;

}
