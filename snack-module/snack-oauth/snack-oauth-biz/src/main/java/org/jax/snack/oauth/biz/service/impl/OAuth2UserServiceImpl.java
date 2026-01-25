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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.oauth.api.dto.OAuth2UserDTO;
import org.jax.snack.oauth.api.service.OAuth2UserService;
import org.jax.snack.oauth.api.vo.OAuth2UserVO;
import org.jax.snack.oauth.biz.converter.OAuth2UserConverter;
import org.jax.snack.oauth.biz.entity.OAuth2User;
import org.jax.snack.oauth.biz.repository.OAuth2UserRepository;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
public class OAuth2UserServiceImpl implements OAuth2UserService {

	private final OAuth2UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final SecurityProperties securityProperties;

	private final OAuth2UserConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(OAuth2UserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, dto.getUsername()).build();
		if (!this.userRepository.queryListByDsl(condition).isEmpty()) {
			throw new IllegalArgumentException("User already exists: " + dto.getUsername());
		}

		OAuth2User user = this.converter.toEntity(dto);
		user.setPassword(this.passwordEncoder.encode(this.securityProperties.getDefaultPassword()));
		user.setEnabled(Status.ENABLED.getCode());
		user.setLocked(YesNoStatus.NO.getCode());
		user.setExpired(Status.ENABLED.getCode());
		user.setInitialPassword(YesNoStatus.YES.getCode());
		user.setLastPasswordResetTime(ZonedDateTime.now());

		this.userRepository.save(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(String username, OAuth2UserDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, username).build();

		OAuth2User existing = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		OAuth2User user = this.converter.toEntity(dto);
		user.setId(existing.getId());

		if (StringUtils.hasText(dto.getPassword())) {
			user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
			user.setInitialPassword(YesNoStatus.NO.getCode());
			user.setLastPasswordResetTime(ZonedDateTime.now());
			user.setLocked(YesNoStatus.NO.getCode());
		}

		WhereCondition where = WhereCondition.builder().eq(OAuth2User.Fields.id, existing.getId()).build();
		this.userRepository.updateByDsl(user, where);
	}

	@Override
	public OAuth2UserVO getByUsername(String username) {
		QueryCondition condition = QueryCondition.builder().eq(OAuth2User.Fields.username, username).build();
		OAuth2User user = this.userRepository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		return this.converter.toVO(user);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void delete(String username) {
		WhereCondition condition = WhereCondition.builder().eq(OAuth2User.Fields.username, username).build();
		this.userRepository.deleteByDsl(condition);
	}

}
