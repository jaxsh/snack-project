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

package org.jax.snack.oauth.biz.repository.impl;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.oauth.biz.converter.OAuthAuthorizationConverter;
import org.jax.snack.oauth.biz.entity.OAuthAuthorization;
import org.jax.snack.oauth.biz.repository.OAuthAuthorizationRepository;

import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 授权服务适配器.
 * <p>
 * 以 @Primary 替换 JdbcOAuth2AuthorizationService，封装仓储与转换逻辑.
 *
 * @author Jax Jiang
 */
@Primary
@Component
@RequiredArgsConstructor
public class OAuthAuthorizationServiceAdapter implements OAuth2AuthorizationService {

	private final OAuthAuthorizationRepository repository;

	private final RegisteredClientRepository registeredClientRepository;

	private final OAuthAuthorizationConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void save(OAuth2Authorization authorization) {
		WhereCondition where = WhereCondition.builder().eq(OAuthAuthorization.Fields.id, authorization.getId()).build();
		this.repository.deleteByDsl(where);
		this.repository.save(this.converter.toEntity(authorization));
	}

	@Override
	public void remove(OAuth2Authorization authorization) {
		WhereCondition where = WhereCondition.builder().eq(OAuthAuthorization.Fields.id, authorization.getId()).build();
		this.repository.deleteByDsl(where);
	}

	@Override
	public OAuth2Authorization findById(String id) {
		return this.repository.findById(id).map(this::toObject).orElse(null);
	}

	@Override
	public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
		QueryCondition condition;
		if (tokenType == null) {
			condition = QueryCondition.builder()
				.or(OAuthAuthorization.Fields.state, token)
				.or(OAuthAuthorization.Fields.authorizationCodeValue, token)
				.or(OAuthAuthorization.Fields.accessTokenValue, token)
				.or(OAuthAuthorization.Fields.oidcIdTokenValue, token)
				.or(OAuthAuthorization.Fields.refreshTokenValue, token)
				.or(OAuthAuthorization.Fields.userCodeValue, token)
				.or(OAuthAuthorization.Fields.deviceCodeValue, token)
				.build();
		}
		else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.state, token).build();
		}
		else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.authorizationCodeValue, token).build();
		}
		else if (OAuth2TokenType.ACCESS_TOKEN.equals(tokenType)) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.accessTokenValue, token).build();
		}
		else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.oidcIdTokenValue, token).build();
		}
		else if (OAuth2TokenType.REFRESH_TOKEN.equals(tokenType)) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.refreshTokenValue, token).build();
		}
		else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.userCodeValue, token).build();
		}
		else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
			condition = QueryCondition.builder().eq(OAuthAuthorization.Fields.deviceCodeValue, token).build();
		}
		else {
			return null;
		}
		return this.repository.queryListByDsl(condition).stream().findFirst().map(this::toObject).orElse(null);
	}

	private OAuth2Authorization toObject(OAuthAuthorization entity) {
		var registeredClient = this.registeredClientRepository.findById(entity.getRegisteredClientId());
		if (registeredClient == null) {
			return null;
		}
		return this.converter.toObject(entity, registeredClient);
	}

}
