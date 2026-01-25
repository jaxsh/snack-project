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

package org.jax.snack.oauth.biz.security;

/**
 * OAuth2 安全相关常量.
 *
 * @author Jax Jiang
 */
public final class OAuth2SecurityConstants {

	private OAuth2SecurityConstants() {
	}

	/**
	 * Spring Security Scope 前缀.
	 */
	public static final String SCOPE_PREFIX = "SCOPE_";

	/**
	 * OAuth2 Scope: 受限改密.
	 */
	public static final String PRE_AUTH_RESET_SCOPE = "pre_auth_reset";

	/**
	 * 基础用户角色.
	 */
	public static final String ROLE_USER = "ROLE_USER";

}
