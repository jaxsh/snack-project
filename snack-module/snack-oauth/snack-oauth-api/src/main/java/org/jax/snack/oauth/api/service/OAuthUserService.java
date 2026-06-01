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

package org.jax.snack.oauth.api.service;

import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.vo.OAuthUserVO;

/**
 * OAuth2 用户服务接口.
 *
 * @author Jax Jiang
 */
public interface OAuthUserService {

	/**
	 * 创建新用户.
	 * @param dto 用户信息 DTO
	 */
	void create(OAuthUserDTO dto);

	/**
	 * 更新用户信息.
	 * @param username 用户名
	 * @param dto 用户信息 DTO
	 */
	void update(String username, OAuthUserDTO dto);

	/**
	 * 根据用户名获取用户信息.
	 * @param username 用户名
	 * @return 用户信息 VO
	 */
	OAuthUserVO getByUsername(String username);

	/**
	 * 删除用户.
	 * @param username 用户名
	 */
	void delete(String username);

	/**
	 * 吊销用户的所有 token.
	 * @param username 用户名
	 */
	void revokeTokens(String username);

}
