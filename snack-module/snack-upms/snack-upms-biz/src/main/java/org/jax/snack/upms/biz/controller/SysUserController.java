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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.core.validation.ValidationGroups.Create;
import org.jax.snack.framework.core.validation.ValidationGroups.Update;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.dto.SysUserOAuthDTO;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysUserVO;
import org.jax.snack.upms.biz.entity.SysUser;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理 Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/users")
@RequiredArgsConstructor
public class SysUserController {

	private final SysUserService service;

	/**
	 * 创建用户.
	 * @param dto 用户 DTO.
	 */
	@PostMapping
	public void create(@Validated(Create.class) @RequestBody SysUserDTO dto) {
		this.service.create(dto);
	}

	/**
	 * 按 ID 查询用户.
	 * @param id 用户 ID
	 * @return 分页结果 (单条数据)
	 */
	@GetMapping("/{id}")
	public PageResult<SysUserVO> getById(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.id, id).build();
		return this.service.queryByDsl(condition);
	}

	/**
	 * 按条件查询用户.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	@PostMapping("/query")
	public PageResult<SysUserVO> query(@RequestBody QueryCondition condition) {
		return this.service.queryByDsl(condition);
	}

	/**
	 * 更新用户.
	 * @param id 用户 ID.
	 * @param dto 用户 DTO.
	 */
	@PutMapping("/{id}")
	public void update(@PathVariable Long id, @Validated(Update.class) @RequestBody SysUserDTO dto) {
		this.service.update(id, dto);
	}

	/**
	 * 删除用户.
	 * @param ids 用户 ID 列表.
	 */
	@DeleteMapping("/{ids}")
	public void delete(@PathVariable List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(SysUser.Fields.id, ids).build();
		this.service.deleteByDsl(condition);
	}

	/**
	 * 解锁用户.
	 * @param id 用户 ID
	 */
	@PatchMapping("/{id}/unlock")
	public void unlock(@PathVariable Long id) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.id, id).build();
		String username = this.service.queryByDsl(condition)
			.getRecords()
			.stream()
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"))
			.getUsername();
		SysUserOAuthDTO dto = new SysUserOAuthDTO();
		dto.setLocked(YesNoStatus.NO.getCode());
		this.service.updateOAuth(username, dto);
	}

	/**
	 * 重置用户密码.
	 * @param id 用户 ID
	 * @param dto 包含新密码的 DTO
	 */
	@PostMapping("/{id}/reset-password")
	public void resetPassword(@PathVariable Long id, @Validated @RequestBody SysUserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.id, id).build();
		String username = this.service.queryByDsl(condition)
			.getRecords()
			.stream()
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"))
			.getUsername();
		SysUserOAuthDTO oauthDto = new SysUserOAuthDTO();
		oauthDto.setPassword(dto.getPassword());
		oauthDto.setInitialPassword(YesNoStatus.YES.getCode());
		oauthDto.setExpired(YesNoStatus.NO.getCode());
		this.service.updateOAuth(username, oauthDto);
	}

	/**
	 * 强退用户会话 (不传 sessionId 时强制下线所有会话并撤销 Token).
	 * @param id 用户 ID
	 * @param sessionId 会话 ID (可选)
	 */
	@DeleteMapping("/{id}/sessions")
	public void revokeSession(@PathVariable Long id, @RequestParam(required = false) String sessionId) {
		this.service.revokeSession(id, sessionId);
	}

	/**
	 * 获取指定用户的详情（包含 OAuth 聚合信息）.
	 * @param username 用户名
	 * @return 用户 VO
	 */
	@GetMapping("/info")
	public SysUserVO info(@RequestParam String username) {
		return this.service.findByUsername(username);
	}

}
