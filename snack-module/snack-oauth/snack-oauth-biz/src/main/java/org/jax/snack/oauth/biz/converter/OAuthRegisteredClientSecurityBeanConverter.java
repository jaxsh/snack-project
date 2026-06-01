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

package org.jax.snack.oauth.biz.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jax.snack.oauth.biz.entity.OAuthRegisteredClient;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 Security 对象与 Entity 转换器.
 * <p>
 * 负责 RegisteredClient (Security Domain) 与 OAuthRegisteredClient (Entity) 之间的转换.
 *
 * @author Jax Jiang
 */
@Component
public class OAuthRegisteredClientSecurityBeanConverter {

	private final JsonMapper jsonMapper;

	/**
	 * 构造函数, 初始化 Jackson 模块.
	 */
	public OAuthRegisteredClientSecurityBeanConverter() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		List<JacksonModule> securityModules = SecurityJacksonModules.getModules(classLoader);

		this.jsonMapper = JsonMapper.builder()
			.addModules(securityModules)
			.addModule(new OAuth2AuthorizationServerJacksonModule())
			.build();
	}

	/**
	 * Entity 转 Domain Object.
	 * @param entity 实体
	 * @return RegisteredClient
	 */
	public RegisteredClient toDomain(OAuthRegisteredClient entity) {
		Set<String> clientAuthenticationMethods = StringUtils
			.commaDelimitedListToSet(entity.getClientAuthenticationMethods());
		Set<String> authorizationGrantTypes = StringUtils.commaDelimitedListToSet(entity.getAuthorizationGrantTypes());
		Set<String> redirectUris = StringUtils.commaDelimitedListToSet(entity.getRedirectUris());
		Set<String> postLogoutRedirectUris = StringUtils.commaDelimitedListToSet(entity.getPostLogoutRedirectUris());
		Set<String> clientScopes = StringUtils.commaDelimitedListToSet(entity.getScopes());

		RegisteredClient.Builder builder = RegisteredClient.withId(entity.getId())
			.clientId(entity.getClientId())
			.clientIdIssuedAt(entity.getClientIdIssuedAt())
			.clientSecret(entity.getClientSecret())
			.clientSecretExpiresAt(entity.getClientSecretExpiresAt())
			.clientName(entity.getClientName())
			.clientAuthenticationMethods((authenticationMethods) -> clientAuthenticationMethods
				.forEach((authenticationMethod) -> authenticationMethods
					.add(resolveClientAuthenticationMethod(authenticationMethod))))
			.authorizationGrantTypes((grantTypes) -> authorizationGrantTypes
				.forEach((grantType) -> grantTypes.add(resolveAuthorizationGrantType(grantType))))
			.redirectUris((uris) -> uris.addAll(redirectUris))
			.postLogoutRedirectUris((uris) -> uris.addAll(postLogoutRedirectUris))
			.scopes((scopes) -> scopes.addAll(clientScopes));

		Map<String, Object> clientSettingsMap = parseMap(entity.getClientSettings());
		builder.clientSettings(ClientSettings.withSettings(clientSettingsMap).build());

		Map<String, Object> tokenSettingsMap = parseMap(entity.getTokenSettings());
		builder.tokenSettings(TokenSettings.withSettings(tokenSettingsMap).build());

		return builder.build();
	}

	/**
	 * Domain Object 转 Entity.
	 * @param registeredClient RegisteredClient
	 * @return Entity
	 */
	public OAuthRegisteredClient toEntity(RegisteredClient registeredClient) {
		List<String> clientAuthenticationMethods = new ArrayList<>(
				registeredClient.getClientAuthenticationMethods().size());
		registeredClient.getClientAuthenticationMethods()
			.forEach((clientAuthenticationMethod) -> clientAuthenticationMethods
				.add(clientAuthenticationMethod.getValue()));

		List<String> authorizationGrantTypes = new ArrayList<>(registeredClient.getAuthorizationGrantTypes().size());
		registeredClient.getAuthorizationGrantTypes()
			.forEach((authorizationGrantType) -> authorizationGrantTypes.add(authorizationGrantType.getValue()));

		OAuthRegisteredClient entity = new OAuthRegisteredClient();
		entity.setId(registeredClient.getId());
		entity.setClientId(registeredClient.getClientId());
		entity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());
		entity.setClientSecret(registeredClient.getClientSecret());
		entity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
		entity.setClientName(registeredClient.getClientName());
		entity
			.setClientAuthenticationMethods(StringUtils.collectionToCommaDelimitedString(clientAuthenticationMethods));
		entity.setAuthorizationGrantTypes(StringUtils.collectionToCommaDelimitedString(authorizationGrantTypes));
		entity.setRedirectUris(StringUtils.collectionToCommaDelimitedString(registeredClient.getRedirectUris()));
		entity.setPostLogoutRedirectUris(
				StringUtils.collectionToCommaDelimitedString(registeredClient.getPostLogoutRedirectUris()));
		entity.setScopes(StringUtils.collectionToCommaDelimitedString(registeredClient.getScopes()));
		entity.setClientSettings(writeMap(registeredClient.getClientSettings().getSettings()));
		entity.setTokenSettings(writeMap(registeredClient.getTokenSettings().getSettings()));

		return entity;
	}

	private Map<String, Object> parseMap(String data) {
		try {
			return this.jsonMapper.readValue(data, new TypeReference<>() {
			});
		}
		catch (JacksonException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	private String writeMap(Map<String, Object> data) {
		try {
			return this.jsonMapper.writeValueAsString(data);
		}
		catch (JacksonException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	private static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
		if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
			return AuthorizationGrantType.AUTHORIZATION_CODE;
		}
		else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
			return AuthorizationGrantType.CLIENT_CREDENTIALS;
		}
		else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
			return AuthorizationGrantType.REFRESH_TOKEN;
		}
		return new AuthorizationGrantType(authorizationGrantType);
	}

	private static ClientAuthenticationMethod resolveClientAuthenticationMethod(String clientAuthenticationMethod) {
		if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
		}
		else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.CLIENT_SECRET_POST;
		}
		else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.NONE;
		}
		return new ClientAuthenticationMethod(clientAuthenticationMethod);
	}

}
