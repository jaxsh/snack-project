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

package org.jax.snack.oauth.biz.controller;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.oauth.api.dto.OAuth2UserDTO;
import org.jax.snack.oauth.api.service.OAuth2UserService;
import org.jax.snack.oauth.api.vo.OAuth2UserVO;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 用户管理控制器 (管理员).
 *
 * @author Jax Jiang
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/oauth2/user")
public class OAuth2UserAdminController {

	private final OAuth2UserService userService;

	/**
	 * 创建 OAuth2 用户.
	 * @param request 创建请求
	 */
	@PostMapping
	public void create(@RequestBody @Validated(Create.class) OAuth2UserDTO request) {
		this.userService.create(request);
	}

	/**
	 * 根据用户名获取用户信息.
	 * @param username 用户名
	 * @return 用户信息 VO
	 */
	@GetMapping("/{username}")
	public OAuth2UserVO getByUsername(@PathVariable String username) {
		return this.userService.getByUsername(username);
	}

	/**
	 * 更新用户信息 (包括密码、邮箱、手机号).
	 * @param username 用户名
	 * @param request 更新请求
	 */
	@PutMapping("/{username}")
	public void update(@PathVariable String username, @RequestBody @Validated(Update.class) OAuth2UserDTO request) {
		this.userService.update(username, request);
	}

	/**
	 * 删除用户.
	 * @param username 用户名
	 */
	@DeleteMapping("/{username}")
	public void delete(@PathVariable String username) {
		this.userService.delete(username);
	}

}
