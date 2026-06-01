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

package org.jax.snack.upms.biz.client;

import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.vo.OAuthUserVO;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 * OAuth2 用户服务 HTTP 客户端.
 *
 * @author Jax Jiang
 */
@HttpExchange("/api/oauth2/user")
public interface OAuth2UserClient {

	/**
	 * 创建新用户.
	 * @param dto 用户信息 DTO
	 */
	@PostExchange
	void create(@RequestBody OAuthUserDTO dto);

	/**
	 * 更新用户信息.
	 * @param username 用户名
	 * @param dto 用户信息 DTO
	 */
	@PutExchange("/{username}")
	void update(@PathVariable String username, @RequestBody OAuthUserDTO dto);

	/**
	 * 根据用户名获取用户信息.
	 * @param username 用户名
	 * @return 用户信息 VO
	 */
	@GetExchange("/{username}")
	OAuthUserVO getByUsername(@PathVariable String username);

	/**
	 * 删除用户.
	 * @param username 用户名
	 */
	@DeleteExchange("/{username}")
	void delete(@PathVariable String username);

	/**
	 * 吊销用户的所有 token.
	 * @param username 用户名
	 */
	@DeleteExchange("/{username}/tokens")
	void revokeTokens(@PathVariable String username);

}
