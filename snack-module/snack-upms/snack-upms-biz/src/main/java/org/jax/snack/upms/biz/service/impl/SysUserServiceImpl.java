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

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.vo.OAuthUserVO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.MfaSetupVO;
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
import org.springframework.util.StringUtils;

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

	private final DefaultCodeVerifier mfaCodeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(),
			new SystemTimeProvider());

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

		if (Objects.equals(dto.getMfaEnabled(), YesNoStatus.YES.getCode())
				&& (!StringUtils.hasText(dto.getMfaSecret()) || !StringUtils.hasText(dto.getMfaCode())
						|| !this.mfaCodeVerifier.isValidCode(dto.getMfaSecret(), dto.getMfaCode()))) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "MFA code");
		}

		SysUser entity = this.converter.toEntity(dto);
		entity.setId(id);
		entity.setUsername(current.getUsername());

		this.repository.update(entity);

		OAuthUserDTO oAuthPatch = new OAuthUserDTO();
		boolean needSync = false;
		if (!ObjectUtils.isEmpty(dto.getStatus()) && !Objects.equals(dto.getStatus(), current.getStatus())) {
			oAuthPatch.setEnabled(dto.getStatus());
			needSync = true;
		}
		if (StringUtils.hasText(dto.getMobile()) && !Objects.equals(dto.getMobile(), current.getMobile())) {
			oAuthPatch.setMobile(dto.getMobile());
			needSync = true;
		}
		if (StringUtils.hasText(dto.getEmail()) && !Objects.equals(dto.getEmail(), current.getEmail())) {
			oAuthPatch.setEmail(dto.getEmail());
			needSync = true;
		}
		if (!ObjectUtils.isEmpty(dto.getExpireDate())) {
			oAuthPatch.setExpireDate(dto.getExpireDate());
			needSync = true;
		}
		if (!ObjectUtils.isEmpty(dto.getMfaEnabled())) {
			oAuthPatch.setMfaEnabled(dto.getMfaEnabled());
			if (Objects.equals(dto.getMfaEnabled(), YesNoStatus.YES.getCode())) {
				oAuthPatch.setMfaSecret(dto.getMfaSecret());
			}
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
		fillRoleCodes(result.getRecords());
		return result;
	}

	private void fillLastActiveTime(List<SysUserVO> records) {
		if (CollectionUtils.isEmpty(records)) {
			return;
		}
		Map<String, ZonedDateTime> lastActiveTimes = this.sessionService.getLastActiveTimes();
		records.forEach((vo) -> vo.setLastActiveTime(lastActiveTimes.get(vo.getUsername())));
	}

	private void fillRoleCodes(List<SysUserVO> records) {
		if (CollectionUtils.isEmpty(records)) {
			return;
		}
		List<String> usernames = records.stream().map(SysUserVO::getUsername).toList();
		WhereCondition whereCondition = WhereCondition.builder().in(SysUserRole.Fields.username, usernames).build();
		QueryCondition queryCondition = QueryCondition.builder().where(whereCondition.getWhere()).build();
		List<SysUserRole> userRoles = this.userRoleRepository.queryListByDsl(queryCondition);
		Map<String, List<String>> rolesByUser = userRoles.stream()
			.collect(Collectors.groupingBy(SysUserRole::getUsername,
					Collectors.mapping(SysUserRole::getRoleCode, Collectors.toList())));
		records.forEach((vo) -> vo.setRoleCodes(rolesByUser.getOrDefault(vo.getUsername(), List.of())));
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
		if (ObjectUtils.isEmpty(user)) {
			return null;
		}
		SysUserVO vo = this.converter.toVO(user);
		vo.setRoleCodes(getUserRoles(user.getUsername()));
		return vo;
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
		SysUserVO vo = this.converter.toVO(list.get(0));
		ApiResponse<OAuthUserVO> oauthResponse = this.oAuth2UserClient.getByUsername(username);
		if (!ObjectUtils.isEmpty(oauthResponse) && !ObjectUtils.isEmpty(oauthResponse.getData())) {
			vo.setMfaEnabled(oauthResponse.getData().getMfaEnabled());
		}
		return vo;
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
		if (!CollectionUtils.isEmpty(dto.getRoleCodes())) {
			this.updateUserRoles(current.getUsername(), dto.getRoleCodes());
		}
		if (!CollectionUtils.isEmpty(dto.getOrgCodes())) {
			this.updateUserOrgs(current.getUsername(), dto.getOrgCodes());
		}
	}

	@Override
	public MfaSetupVO mfaSetup(String username) {
		String secret = new DefaultSecretGenerator().generate();
		QrData qrData = new QrData.Builder().label(username)
			.secret(secret)
			.issuer("snack")
			.digits(6)
			.period(30)
			.build();
		return new MfaSetupVO(secret, qrData.getUri());
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
