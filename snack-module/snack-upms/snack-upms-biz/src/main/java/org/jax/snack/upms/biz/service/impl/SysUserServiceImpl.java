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

package org.jax.snack.upms.biz.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.oauth.api.dto.OAuth2UserDTO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.enums.Status;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysUserVO;
import org.jax.snack.upms.biz.client.OAuth2UserClient;
import org.jax.snack.upms.biz.converter.SysResourceConverter;
import org.jax.snack.upms.biz.converter.SysUserConverter;
import org.jax.snack.upms.biz.entity.SysResource;
import org.jax.snack.upms.biz.entity.SysRole;
import org.jax.snack.upms.biz.entity.SysUser;
import org.jax.snack.upms.biz.entity.SysUserOrg;
import org.jax.snack.upms.biz.entity.SysUserRole;
import org.jax.snack.upms.biz.repository.SysResourceRepository;
import org.jax.snack.upms.biz.repository.SysRoleRepository;
import org.jax.snack.upms.biz.repository.SysUserOrgRepository;
import org.jax.snack.upms.biz.repository.SysUserRepository;
import org.jax.snack.upms.biz.repository.SysUserRoleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 用户服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

	private final SysUserRepository repository;

	private final SysUserRoleRepository userRoleRepository;

	private final SysUserOrgRepository userOrgRepository;

	private final SysResourceRepository resourceRepository;

	private final SysRoleRepository roleRepository;

	private final SysUserConverter converter;

	private final SysResourceConverter resourceConverter;

	private final OAuth2UserClient oAuth2UserClient;

	private final TransactionTemplate transactionTemplate;

	@Override
	public void create(SysUserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, dto.getUsername()).build();
		if (this.repository.existsByDsl(condition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Username");
		}
		OAuth2UserDTO oauthUserDto = new OAuth2UserDTO();
		oauthUserDto.setUsername(dto.getUsername());
		this.oAuth2UserClient.create(oauthUserDto);

		this.transactionTemplate.executeWithoutResult((status) -> {
			SysUser entity = this.converter.toEntity(dto);
			this.repository.save(entity);

			if (!CollectionUtils.isEmpty(dto.getRoleCodes())) {
				validateRoleStatus(dto.getRoleCodes());
				Set<SysUserRole> relations = dto.getRoleCodes().stream().map((roleCode) -> {
					SysUserRole relation = new SysUserRole();
					relation.setUsername(dto.getUsername());
					relation.setRoleCode(roleCode);
					return relation;
				}).collect(Collectors.toSet());
				this.userRoleRepository.saveBatch(relations);
			}

			if (!CollectionUtils.isEmpty(dto.getOrgCodes())) {
				Set<SysUserOrg> relations = dto.getOrgCodes().stream().map((orgCode) -> {
					SysUserOrg relation = new SysUserOrg();
					relation.setUsername(dto.getUsername());
					relation.setOrgCode(orgCode);
					return relation;
				}).collect(Collectors.toSet());
				this.userOrgRepository.saveBatch(relations);
			}
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysUserDTO dto) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"));

		SysUser entity = this.converter.toEntity(dto);
		entity.setId(id);
		entity.setUsername(current.getUsername());

		this.repository.update(entity);

		Optional.ofNullable(dto.getRoleCodes()).ifPresent((roleCodes) -> {
			deleteUserRolesByUsername(current.getUsername());
			if (!CollectionUtils.isEmpty(roleCodes)) {
				Set<SysUserRole> relations = roleCodes.stream().map((roleCode) -> {
					SysUserRole relation = new SysUserRole();
					relation.setUsername(current.getUsername());
					relation.setRoleCode(roleCode);
					return relation;
				}).collect(Collectors.toSet());
				this.userRoleRepository.saveBatch(relations);
			}
		});

		Optional.ofNullable(dto.getOrgCodes()).ifPresent((orgCodes) -> {
			deleteUserOrgsByUsername(current.getUsername());
			if (!CollectionUtils.isEmpty(orgCodes)) {
				Set<SysUserOrg> relations = orgCodes.stream().map((orgCode) -> {
					SysUserOrg relation = new SysUserOrg();
					relation.setUsername(current.getUsername());
					relation.setOrgCode(orgCode);
					return relation;
				}).collect(Collectors.toSet());
				this.userOrgRepository.saveBatch(relations);
			}
		});
	}

	@Override
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		List<SysUser> users = this.repository.queryListByDsl(queryCondition);
		if (CollectionUtils.isEmpty(users)) {
			return;
		}

		this.transactionTemplate.executeWithoutResult((status) -> {
			users.forEach((current) -> {
				deleteUserRolesByUsername(current.getUsername());
				deleteUserOrgsByUsername(current.getUsername());
			});
			this.repository.deleteByDsl(condition);
		});

		users.forEach((current) -> this.oAuth2UserClient.delete(current.getUsername()));
	}

	private void deleteUserRolesByUsername(String username) {
		WhereCondition condition = WhereCondition.builder().eq(SysUserRole.Fields.username, username).build();
		this.userRoleRepository.deleteByDsl(condition);
	}

	private void deleteUserOrgsByUsername(String username) {
		WhereCondition condition = WhereCondition.builder().eq(SysUserOrg.Fields.username, username).build();
		this.userOrgRepository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysUserVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<SysResourceVO> getUserResources(String username) {
		List<SysResource> resources = this.resourceRepository.selectResourcesByUsername(username);
		return resources.stream().map(this.resourceConverter::toVO).toList();
	}

	@Override
	public SysUserVO queryById(Long id) {
		SysUser user = this.repository.findById(id).orElse(null);
		return this.converter.toVO(user);
	}

	@Override
	public SysUserVO getByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, username).build();
		List<SysUser> list = this.repository.queryListByDsl(condition);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return this.converter.toVO(list.get(0));
	}

	private void validateRoleStatus(Set<String> roleCodes) {
		QueryCondition condition = QueryCondition.builder()
			.in(SysRole.Fields.roleCode, roleCodes.stream().toList())
			.build();
		List<SysRole> roles = this.roleRepository.queryListByDsl(condition);

		roles.stream()
			.filter((role) -> Status.DISABLED.getCode().equals(role.getStatus()))
			.findFirst()
			.ifPresent((role) -> {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "Role " + role.getRoleCode() + " is disabled");
			});
	}

}
