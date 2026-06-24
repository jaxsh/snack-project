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
import org.jax.snack.framework.core.api.query.UpdateCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.vo.MfaQrVO;
import org.jax.snack.oauth.api.vo.OAuthUserVO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.service.SysResourceService;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.MfaSetupVO;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.jax.snack.upms.api.vo.SysUserVO;
import org.jax.snack.upms.biz.client.OAuth2UserClient;
import org.jax.snack.upms.biz.converter.SysUserConverter;
import org.jax.snack.upms.biz.entity.SysUser;
import org.jax.snack.upms.biz.entity.SysUserOrg;
import org.jax.snack.upms.biz.entity.SysUserRole;
import org.jax.snack.upms.biz.repository.SysUserOrgRepository;
import org.jax.snack.upms.biz.repository.SysUserRepository;
import org.jax.snack.upms.biz.repository.SysUserRoleRepository;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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

	private static final String USER_ENTITY = "User";

	private final SysUserRepository repository;

	private final SysUserRoleRepository userRoleRepository;

	private final SysUserOrgRepository userOrgRepository;

	private final SysResourceService sysResourceService;

	private final SysUserConverter converter;

	private final OAuth2UserClient oAuth2UserClient;

	private final SysSessionService sessionService;

	private final TransactionTemplate transactionTemplate;

	@Value("${spring.application.name}")
	private String applicationName;

	private final ObjectProvider<SysUserService> selfProvider;

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

		updateUserRoles(dto.getUsername(), dto.getRoleCodes().orElse(Set.of()));
		updateUserOrgs(dto.getUsername(), dto.getOrgCodes().orElse(Set.of()));
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
	public void updateOAuth(String username, OAuthUserDTO dto) {
		this.oAuth2UserClient.update(username, dto);
	}

	@Override
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

	private void updateUserRoles(String username, Set<String> roleCodes) {
		deleteUserRolesByUsername(username);
		if (!CollectionUtils.isEmpty(roleCodes)) {
			Set<SysUserRole> relations = roleCodes.stream().map((roleCode) -> {
				SysUserRole relation = new SysUserRole();
				relation.setUsername(username);
				relation.setRoleCode(roleCode);
				return relation;
			}).collect(Collectors.toSet());
			this.userRoleRepository.saveBatch(relations);
		}
	}

	private void updateUserOrgs(String username, Set<String> orgCodes) {
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
	public void update(Long id, SysUserDTO dto) {
		SysUser current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, USER_ENTITY));

		if (Objects.equals(dto.getMfaEnabled(), YesNoStatus.YES.getCode()) && !StringUtils.hasText(dto.getMfaCode())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "MFA code");
		}

		SysUser entity = this.converter.toEntity(dto);
		entity.setUsername(current.getUsername());
		this.repository.updateByDsl(entity, UpdateCondition.builder().setNulls(dto).eq(SysUser.Fields.id, id).build());

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
		if (dto.getExpireDate().isPresent()) {
			oAuthPatch.setExpireDate(dto.getExpireDate());
			needSync = true;
		}
		if (!ObjectUtils.isEmpty(dto.getMfaEnabled())) {
			oAuthPatch.setMfaEnabled(dto.getMfaEnabled());
			if (StringUtils.hasText(dto.getMfaCode())) {
				oAuthPatch.setMfaCode(dto.getMfaCode());
			}
			needSync = true;
		}
		if (needSync) {
			this.oAuth2UserClient.update(current.getUsername(), oAuthPatch);
		}

		if (dto.getRoleCodes().isPresent()) {
			this.updateUserRoles(current.getUsername(), dto.getRoleCodes().orElse(Set.of()));
		}
		if (dto.getOrgCodes().isPresent()) {
			updateUserOrgs(current.getUsername(), dto.getOrgCodes().get());
		}
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'enabled-roles:' + #username")
	public List<String> getEnabledRoleCodesByUsername(String username) {
		return this.userRoleRepository.selectEnabledRoleCodesByUsername(username, Status.ENABLED.getCode());
	}

	@Override
	public List<SysResourceVO> getResourcesByUsername(String username) {
		return this.selfProvider.getObject()
			.getEnabledRoleCodesByUsername(username)
			.stream()
			.flatMap((code) -> this.sysResourceService.getResourcesByRoleCode(code).stream())
			.distinct()
			.toList();
	}

	@Override
	public List<SysResourceVO> getUserResources(String username) {
		return this.getResourcesByUsername(username);
	}

	@Override
	public SysUserVO findByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(SysUser.Fields.username, username).build();
		SysUserVO vo = this.queryByDsl(condition)
			.getRecords()
			.stream()
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"));
		ApiResponse<OAuthUserVO> oauthResponse = this.oAuth2UserClient.getByUsername(username);
		if (!ObjectUtils.isEmpty(oauthResponse) && !ObjectUtils.isEmpty(oauthResponse.getData())) {
			vo.setOauthVO(oauthResponse.getData());
		}
		return vo;
	}

	@Override
	public MfaSetupVO mfaSetup(String username) {
		ApiResponse<MfaQrVO> resp = this.oAuth2UserClient.getMfaQrUri(username, this.applicationName);
		return new MfaSetupVO(null, resp.getData().getQrUri());
	}

}
