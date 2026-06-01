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
import org.jax.snack.oauth.biz.converter.OAuthRegisteredClientSecurityBeanConverter;
import org.jax.snack.oauth.biz.entity.OAuthRegisteredClient;
import org.jax.snack.oauth.biz.repository.OAuthRegisteredClientRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * OAuth2 客户端安全框架适配器.
 * <p>
 * 专门实现 Spring Security 的 RegisteredClientRepository 接口，封装模型转换与缓存逻辑.
 *
 * @author Jax Jiang
 */
@Primary
@Component
@RequiredArgsConstructor
public class OAuthRegisteredClientSecurityAdapter implements RegisteredClientRepository {

	private static final String CACHE_NAME = "registered_clients";

	private final OAuthRegisteredClientRepository repository;

	private final OAuthRegisteredClientSecurityBeanConverter converter;

	@Override
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void save(RegisteredClient registeredClient) {
		OAuthRegisteredClient entity = this.converter.toEntity(registeredClient);
		this.repository.findById(entity.getId())
			.ifPresentOrElse((existing) -> this.repository.update(entity), () -> this.repository.save(entity));
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
	public RegisteredClient findById(String id) {
		return this.repository.findById(id).map(this.converter::toDomain).orElse(null);
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "#clientId", unless = "#result == null")
	public RegisteredClient findByClientId(String clientId) {
		QueryCondition condition = QueryCondition.builder().eq(OAuthRegisteredClient.Fields.clientId, clientId).build();
		return this.repository.queryListByDsl(condition)
			.stream()
			.findFirst()
			.map(this.converter::toDomain)
			.orElse(null);
	}

}
