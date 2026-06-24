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

package org.jax.snack.upms.biz.controller;

import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.MfaSetupVO;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysUserVO;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户自服务 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/user")
@RequiredArgsConstructor
public class SysProfileController {

	private final SysUserService sysUserService;

	/**
	 * 获取当前用户信息.
	 * @return 用户 VO
	 */
	@GetMapping
	public SysUserVO info() {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		return this.sysUserService.findByUsername(username);
	}

	/**
	 * 更新当前登录用户的个人基本资料.
	 * @param dto 用户 DTO (仅修改个人允许修改的字段)
	 */
	@PutMapping
	public void updateProfile(@Validated(Update.class) @RequestBody SysUserDTO dto) {
		if (Objects.equals(dto.getMfaEnabled(), YesNoStatus.NO.getCode()) && !StringUtils.hasText(dto.getMfaCode())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "MFA code");
		}
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		Long id = this.sysUserService.findByUsername(username).getId();
		this.sysUserService.update(id, dto);
	}

	/**
	 * 修改当前登录用户的密码.
	 * @param dto 包含新密码的 DTO
	 */
	@PutMapping("/password")
	public void changePassword(@Validated @RequestBody SysUserDTO dto) {
		if (!StringUtils.hasText(dto.getPassword())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "password");
		}
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		Long id = this.sysUserService.findByUsername(username).getId();
		OAuthUserDTO oauthDto = new OAuthUserDTO();
		oauthDto.setPassword(dto.getPassword());
		oauthDto.setInitialPassword(YesNoStatus.NO.getCode());
		oauthDto.setExpired(YesNoStatus.NO.getCode());
		this.sysUserService.updateOAuth(id, oauthDto);
	}

	/**
	 * 获取当前用户 MFA 初始化信息.
	 * @return MFA 初始化信息
	 */
	@GetMapping("/mfa")
	public MfaSetupVO mfaSetup() {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		return this.sysUserService.mfaSetup(username);
	}

	/**
	 * 获取当前用户可访问的资源集合.
	 * @return 资源 VO 列表
	 */
	@GetMapping("/resources")
	public List<SysResourceVO> getResources() {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		return this.sysUserService.getUserResources(username);
	}

}
