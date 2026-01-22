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

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.oauth.api.dto.RegisteredClientDTO;
import org.jax.snack.oauth.api.service.OAuth2RegisteredClientService;
import org.jax.snack.oauth.api.vo.RegisteredClientVO;
import org.jax.snack.oauth.biz.converter.OAuth2RegisteredClientConverter;
import org.jax.snack.oauth.biz.entity.OAuth2RegisteredClient;
import org.jax.snack.oauth.biz.repository.OAuth2RegisteredClientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * OAuth2 客户端服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class OAuth2RegisteredClientServiceImpl implements OAuth2RegisteredClientService {

	private final OAuth2RegisteredClientRepository repository;

	private final OAuth2RegisteredClientConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(RegisteredClientDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(OAuth2RegisteredClient.Fields.clientId, dto.getClientId())
			.build();
		boolean exists = this.repository.existsByDsl(existsCondition);
		if (exists) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Client ID");
		}

		OAuth2RegisteredClient entity = this.converter.toEntity(dto);
		entity.setClientIdIssuedAt(Instant.now());
		this.repository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(String id, RegisteredClientDTO dto) {
		OAuth2RegisteredClient existing = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Registered Client"));

		OAuth2RegisteredClient entity = this.converter.toEntity(dto);
		entity.setId(id);
		entity.setClientIdIssuedAt(existing.getClientIdIssuedAt());
		if (entity.getClientSecret() == null) {
			entity.setClientSecret(existing.getClientSecret());
		}

		this.repository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteById(String id) {
		this.repository.deleteById(id);
	}

	@Override
	public PageResult<RegisteredClientVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<RegisteredClientVO> getByClientId(String clientId) {
		QueryCondition condition = QueryCondition.builder()
			.eq(OAuth2RegisteredClient.Fields.clientId, clientId)
			.build();
		List<OAuth2RegisteredClient> list = this.repository.queryListByDsl(condition);
		return list.stream().map(this.converter::toVO).toList();
	}

}
