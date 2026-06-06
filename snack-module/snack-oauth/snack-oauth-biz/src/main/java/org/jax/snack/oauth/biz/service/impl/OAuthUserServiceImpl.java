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

package org.jax.snack.oauth.biz.service.impl;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.enums.BaseEnum;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.service.OAuthUserService;
import org.jax.snack.oauth.api.vo.OAuthUserVO;
import org.jax.snack.oauth.biz.converter.OAuthUserConverter;
import org.jax.snack.oauth.biz.entity.OAuthAuthorization;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.jax.snack.oauth.biz.repository.OAuthAuthorizationRepository;
import org.jax.snack.oauth.biz.repository.OAuthUserRepository;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * OAuth2 用户管理服务实现类.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class OAuthUserServiceImpl implements OAuthUserService {

	private final OAuthUserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final SecurityProperties securityProperties;

	private final OAuthUserConverter converter;

	private final OAuthAuthorizationRepository authorizationRepository;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(OAuthUserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(OAuthUser.Fields.username, dto.getUsername()).build();
		if (!this.userRepository.queryListByDsl(condition).isEmpty()) {
			throw new IllegalArgumentException("User already exists: " + dto.getUsername());
		}

		OAuthUser user = this.converter.toEntity(dto);
		user.setPassword(this.passwordEncoder.encode(this.securityProperties.getDefaultPassword()));
		user.setEnabled(Status.ENABLED.getCode());
		user.setLocked(YesNoStatus.NO.getCode());
		user.setExpired(YesNoStatus.NO.getCode());
		user.setInitialPassword(YesNoStatus.YES.getCode());
		user.setLastPasswordResetTime(ZonedDateTime.now());

		this.userRepository.save(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(String username, OAuthUserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(OAuthUser.Fields.username, username).build();

		OAuthUser existing = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"));

		WhereCondition where = WhereCondition.builder().eq(OAuthUser.Fields.id, existing.getId()).build();

		if (Objects.equals(dto.getLocked(), YesNoStatus.NO.getCode())) {
			Map<String, Object> setData = new HashMap<>();
			setData.put(OAuthUser.Fields.locked, YesNoStatus.NO.getCode());
			setData.put(OAuthUser.Fields.lockCount, 0);
			setData.put(OAuthUser.Fields.lockUntil, null);
			this.userRepository.updateByDsl(setData, where);
		}
		else {
			OAuthUser user = this.converter.toEntity(dto);
			user.setId(existing.getId());

			if (StringUtils.hasText(dto.getPassword())) {
				user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
				user.setLastPasswordResetTime(ZonedDateTime.now());
				user.setLocked(YesNoStatus.NO.getCode());
				user.setExpired(YesNoStatus.NO.getCode());
				if (dto.getInitialPassword() == null) {
					user.setInitialPassword(YesNoStatus.NO.getCode());
				}
			}

			this.userRepository.updateByDsl(user, where);
		}

		if (StringUtils.hasText(dto.getPassword())
				&& Objects.equals(dto.getInitialPassword(), YesNoStatus.YES.getCode())) {
			revokeTokens(username);
		}

		if (Objects.equals(dto.getEnabled(), Status.DISABLED.getCode())) {
			revokeTokens(username);
		}
	}

	@Override
	public OAuthUserVO getByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(OAuthUser.Fields.username, username).build();
		OAuthUser user = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "User"));

		OAuthUserVO vo = this.converter.toVO(user);
		boolean credentialsExpired = this.securityProperties.isCredentialsExpired(user.getLastPasswordResetTime());
		int expiredCode = credentialsExpired ? YesNoStatus.YES.getCode() : YesNoStatus.NO.getCode();
		vo.setExpired(expiredCode);
		vo.setExpiredLabel(BaseEnum.getNameByCode(YesNoStatus.class, expiredCode));
		return vo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(String username) {
		WhereCondition condition = WhereCondition.builder().eq(OAuthUser.Fields.username, username).build();
		this.userRepository.deleteByDsl(condition);
	}

	@Override
	public void revokeTokens(String username) {
		WhereCondition condition = WhereCondition.builder()
			.eq(OAuthAuthorization.Fields.principalName, username)
			.build();
		this.authorizationRepository.deleteByDsl(condition);
	}

}
