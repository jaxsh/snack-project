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

import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.service.OAuthUserService;
import org.jax.snack.oauth.api.vo.OAuthUserVO;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户资料 Controller.
 * <p>
 * 提供当前登录用户的个人资料查询和更新功能.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/oauth/user")
@RequiredArgsConstructor
public class OAuthUserProfileController {

	private final OAuthUserService userService;

	/**
	 * 获取当前用户资料.
	 * @param principal 当前登录的用户主体
	 * @return 用户资料 VO
	 */
	@GetMapping("/profile")
	public OAuthUserVO getProfile(Principal principal) {
		return this.userService.getByUsername(principal.getName());
	}

	/**
	 * 更新当前用户资料.
	 * @param principal 当前登录的用户主体
	 * @param dto 用户信息 DTO
	 */
	@PutMapping("/profile")
	public void updateProfile(Principal principal, @RequestBody OAuthUserDTO dto) {
		this.userService.update(principal.getName(), dto);
	}

}
