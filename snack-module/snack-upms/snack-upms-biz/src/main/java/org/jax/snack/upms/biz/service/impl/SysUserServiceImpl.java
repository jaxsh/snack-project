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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysSessionVO;
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

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

	private static final String CACHE_NAME = "upms:user";

	private static final String UNLESS_NULL = "#result == null";

	private static final String USER_ENTITY = "User";

	private final SysUserRepository repository;

	private final SysUserRoleRepository userRoleRepository;

	private final SysUserOrgRepository userOrgRepository;

	private final SysResourceRepository resourceRepository;

	private final SysRoleRepository roleRepository;

	private final SysUserConverter converter;

	private final SysResourceConverter resourceConverter;

	private final OAuth2UserClient oAuth2UserClient;

	private final SysSessionService sessionService;

	private final TransactionTemplate transactionTemplate;

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void create(SysUserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, dto.getUsername()).build();
		if (this.repository.existsByDsl(condition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Username");
		}
		OAuthUserDTO oauthUserDto = new OAuthUserDTO();
		oauthUserDto.setUsername(dto.getUsername());
		oauthUserDto.setMobile(dto.getMobile());
		oauthUserDto.setEmail(dto.getEmail());
		this.oAuth2UserClient.create(oauthUserDto);

		SysUser entity = this.converter.toEntity(dto);
		this.repository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void update(Long id, SysUserDTO dto) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));

		SysUser entity = this.converter.toEntity(dto);
		entity.setId(id);
		entity.setUsername(current.getUsername());

		this.repository.update(entity);

		OAuthUserDTO oAuthPatch = new OAuthUserDTO();
		boolean needSync = false;
		if (dto.getStatus() != null && !Objects.equals(dto.getStatus(), current.getStatus())) {
			oAuthPatch.setEnabled(dto.getStatus());
			needSync = true;
		}
		if (dto.getMobile() != null && !Objects.equals(dto.getMobile(), current.getMobile())) {
			oAuthPatch.setMobile(dto.getMobile());
			needSync = true;
		}
		if (dto.getEmail() != null && !Objects.equals(dto.getEmail(), current.getEmail())) {
			oAuthPatch.setEmail(dto.getEmail());
			needSync = true;
		}
		if (dto.getExpireDate() != null) {
			oAuthPatch.setExpireDate(dto.getExpireDate());
			needSync = true;
		}
		if (needSync) {
			this.oAuth2UserClient.update(current.getUsername(), oAuthPatch);
		}
	}

	@Override
	@CacheEvict(value = CACHE_NAME, allEntries = true)
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
		PageResult<SysUserVO> result;
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			result = this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			result = this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
		fillLastActiveTime(result.getRecords());
		return result;
	}

	private void fillLastActiveTime(List<SysUserVO> records) {
		if (CollectionUtils.isEmpty(records)) {
			return;
		}
		Map<String, ZonedDateTime> lastActiveTimes = this.sessionService.getLastActiveTimes();
		records.forEach((vo) -> vo.setLastActiveTime(lastActiveTimes.get(vo.getUsername())));
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'resources:' + #username", unless = UNLESS_NULL)
	public List<SysResourceVO> getUserResources(String username) {
		List<SysResource> resources = this.resourceRepository.selectResourcesByUsername(username);
		return resources.stream().map(this.resourceConverter::toVO).toList();
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'id:' + #id", unless = UNLESS_NULL)
	public SysUserVO queryById(Long id) {
		SysUser user = this.repository.findById(id).orElse(null);
		return this.converter.toVO(user);
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'roles:' + #username", unless = UNLESS_NULL)
	public List<String> getUserRoles(String username) {
		WhereCondition condition = WhereCondition.builder().eq(SysUserRole.Fields.username, username).build();
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		return this.userRoleRepository.queryListByDsl(queryCondition).stream().map(SysUserRole::getRoleCode).toList();
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'info:' + #username", unless = UNLESS_NULL)
	public SysUserVO getByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, username).build();
		List<SysUser> list = this.repository.queryListByDsl(condition);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		return this.converter.toVO(list.get(0));
	}

	@Override
	public void unlock(Long id) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));
		OAuthUserDTO dto = new OAuthUserDTO();
		dto.setLocked(YesNoStatus.NO.getCode());
		this.oAuth2UserClient.update(current.getUsername(), dto);
	}

	@Override
	public void resetPassword(Long id, String newPassword, YesNoStatus initialPassword, YesNoStatus expired) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));
		OAuthUserDTO dto = new OAuthUserDTO();
		dto.setPassword(newPassword);
		dto.setInitialPassword(initialPassword.getCode());
		dto.setExpired(expired.getCode());
		this.oAuth2UserClient.update(current.getUsername(), dto);
	}

	@Override
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void revokeTokens(Long id) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));
		this.oAuth2UserClient.revokeTokens(current.getUsername());
		this.sessionService.revokeSessionsByUsername(current.getUsername());
	}

	@Override
	public List<SysSessionVO> getSessions(Long id) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));
		return this.sessionService.getSessions(current.getUsername());
	}

	@Override
	public void revokeSession(Long id, String sessionId) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));
		this.sessionService.revokeSession(current.getUsername(), sessionId);
	}

	@Override
	public void updateUserRoles(String username, Set<String> roleCodes) {
		deleteUserRolesByUsername(username);
		if (!CollectionUtils.isEmpty(roleCodes)) {
			validateRoleStatus(roleCodes);
			Set<SysUserRole> relations = roleCodes.stream().map((roleCode) -> {
				SysUserRole relation = new SysUserRole();
				relation.setUsername(username);
				relation.setRoleCode(roleCode);
				return relation;
			}).collect(Collectors.toSet());
			this.userRoleRepository.saveBatch(relations);
		}
	}

	@Override
	public void updateUserOrgs(String username, Set<String> orgCodes) {
		deleteUserOrgsByUsername(username);
		if (!CollectionUtils.isEmpty(orgCodes)) {
			Set<SysUserOrg> relations = orgCodes.stream().map((orgCode) -> {
				SysUserOrg relation = new SysUserOrg();
				relation.setUsername(username);
				relation.setOrgCode(orgCode);
				return relation;
			}).collect(Collectors.toSet());
			this.userOrgRepository.saveBatch(relations);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void createWithRelations(SysUserDTO dto) {
		this.create(dto);
		this.updateUserRoles(dto.getUsername(), dto.getRoleCodes());
		this.updateUserOrgs(dto.getUsername(), dto.getOrgCodes());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void updateWithRelations(Long id, SysUserDTO dto) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));

		this.update(id, dto);
		if (dto.getRoleCodes() != null) {
			this.updateUserRoles(current.getUsername(), dto.getRoleCodes());
		}
		if (dto.getOrgCodes() != null) {
			this.updateUserOrgs(current.getUsername(), dto.getOrgCodes());
		}
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
