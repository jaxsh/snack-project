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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jax.snack.oauth.biz.entity.OAuthAuthorization;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.security.jackson.SecurityJacksonModules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2 授权 Security 对象与 Entity 转换器.
 * <p>
 * 负责 OAuth2Authorization (Security Domain) 与 OAuthAuthorization (Entity) 之间的转换.
 *
 * @author Jax Jiang
 */
@Component
public class OAuthAuthorizationConverter {

	private final JsonMapper jsonMapper;

	/**
	 * 构造函数，初始化 Jackson 模块.
	 */
	public OAuthAuthorizationConverter() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		List<JacksonModule> securityModules = SecurityJacksonModules.getModules(classLoader);

		this.jsonMapper = JsonMapper.builder()
			.addModules(securityModules)
			.addModule(new OAuth2AuthorizationServerJacksonModule())
			.build();
	}

	/**
	 * Security Domain 转 Entity.
	 * @param domain Spring Security OAuth2Authorization
	 * @return entity
	 */
	public OAuthAuthorization toEntity(OAuth2Authorization domain) {
		OAuthAuthorization entity = new OAuthAuthorization();
		entity.setId(domain.getId());
		entity.setRegisteredClientId(domain.getRegisteredClientId());
		entity.setPrincipalName(domain.getPrincipalName());
		entity.setAuthorizationGrantType(domain.getAuthorizationGrantType().getValue());
		entity.setAuthorizedScopes(StringUtils.collectionToCommaDelimitedString(domain.getAuthorizedScopes()));
		entity.setAttributes(writeMap(domain.getAttributes()));
		entity.setState(domain.getAttribute(OAuth2ParameterNames.STATE));

		OAuth2Authorization.Token<OAuth2AuthorizationCode> authCode = domain.getToken(OAuth2AuthorizationCode.class);
		if (authCode != null) {
			OAuth2AuthorizationCode code = authCode.getToken();
			entity.setAuthorizationCodeValue(code.getTokenValue());
			entity.setAuthorizationCodeIssuedAt(code.getIssuedAt());
			entity.setAuthorizationCodeExpiresAt(code.getExpiresAt());
			entity.setAuthorizationCodeMetadata(writeMap(authCode.getMetadata()));
		}

		OAuth2Authorization.Token<OAuth2AccessToken> accessToken = domain.getAccessToken();
		if (accessToken != null) {
			OAuth2AccessToken token = accessToken.getToken();
			entity.setAccessTokenValue(token.getTokenValue());
			entity.setAccessTokenIssuedAt(token.getIssuedAt());
			entity.setAccessTokenExpiresAt(token.getExpiresAt());
			entity.setAccessTokenType(token.getTokenType().getValue());
			entity.setAccessTokenScopes(StringUtils.collectionToCommaDelimitedString(token.getScopes()));
			entity.setAccessTokenMetadata(writeMap(accessToken.getMetadata()));
		}

		OAuth2Authorization.Token<OidcIdToken> oidcTokenWrapper = domain.getToken(OidcIdToken.class);
		if (oidcTokenWrapper != null) {
			OidcIdToken oidcToken = oidcTokenWrapper.getToken();
			entity.setOidcIdTokenValue(oidcToken.getTokenValue());
			entity.setOidcIdTokenIssuedAt(oidcToken.getIssuedAt());
			entity.setOidcIdTokenExpiresAt(oidcToken.getExpiresAt());
			entity.setOidcIdTokenClaims(writeMap(oidcToken.getClaims()));
			entity.setOidcIdTokenMetadata(writeMap(oidcTokenWrapper.getMetadata()));
		}

		OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken = domain.getRefreshToken();
		if (refreshToken != null) {
			OAuth2RefreshToken token = refreshToken.getToken();
			entity.setRefreshTokenValue(token.getTokenValue());
			entity.setRefreshTokenIssuedAt(token.getIssuedAt());
			entity.setRefreshTokenExpiresAt(token.getExpiresAt());
			entity.setRefreshTokenMetadata(writeMap(refreshToken.getMetadata()));
		}

		return entity;
	}

	/**
	 * Entity 转 Security Domain.
	 * @param entity OAuthAuthorization entity
	 * @param registeredClient 已注册客户端
	 * @return Spring Security OAuth2Authorization
	 */
	public OAuth2Authorization toObject(OAuthAuthorization entity, RegisteredClient registeredClient) {
		Set<String> authorizedScopes = StringUtils.commaDelimitedListToSet(entity.getAuthorizedScopes());
		OAuth2Authorization.Builder builder = OAuth2Authorization.withRegisteredClient(registeredClient)
			.id(entity.getId())
			.principalName(entity.getPrincipalName())
			.authorizationGrantType(resolveGrantType(entity.getAuthorizationGrantType()))
			.authorizedScopes(authorizedScopes)
			.attributes((attrs) -> attrs.putAll(parseMap(entity.getAttributes())));

		if (StringUtils.hasText(entity.getAuthorizationCodeValue())) {
			OAuth2AuthorizationCode authCode = new OAuth2AuthorizationCode(entity.getAuthorizationCodeValue(),
					entity.getAuthorizationCodeIssuedAt(), entity.getAuthorizationCodeExpiresAt());
			builder.token(authCode, (metadata) -> metadata.putAll(parseMap(entity.getAuthorizationCodeMetadata())));
		}

		if (StringUtils.hasText(entity.getAccessTokenValue())) {
			Set<String> scopes = StringUtils.commaDelimitedListToSet(entity.getAccessTokenScopes());
			OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
					entity.getAccessTokenValue(), entity.getAccessTokenIssuedAt(), entity.getAccessTokenExpiresAt(),
					scopes);
			builder.token(accessToken, (metadata) -> metadata.putAll(parseMap(entity.getAccessTokenMetadata())));
		}

		if (StringUtils.hasText(entity.getOidcIdTokenValue())) {
			OidcIdToken oidcIdToken = new OidcIdToken(entity.getOidcIdTokenValue(), entity.getOidcIdTokenIssuedAt(),
					entity.getOidcIdTokenExpiresAt(), parseMap(entity.getOidcIdTokenClaims()));
			builder.token(oidcIdToken, (metadata) -> metadata.putAll(parseMap(entity.getOidcIdTokenMetadata())));
		}

		if (StringUtils.hasText(entity.getRefreshTokenValue())) {
			OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(entity.getRefreshTokenValue(),
					entity.getRefreshTokenIssuedAt(), entity.getRefreshTokenExpiresAt());
			builder.token(refreshToken, (metadata) -> metadata.putAll(parseMap(entity.getRefreshTokenMetadata())));
		}

		return builder.build();
	}

	private Map<String, Object> parseMap(String data) {
		if (!StringUtils.hasText(data)) {
			return new HashMap<>();
		}
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

	private static AuthorizationGrantType resolveGrantType(String value) {
		if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(value)) {
			return AuthorizationGrantType.AUTHORIZATION_CODE;
		}
		if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(value)) {
			return AuthorizationGrantType.CLIENT_CREDENTIALS;
		}
		if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(value)) {
			return AuthorizationGrantType.REFRESH_TOKEN;
		}
		if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(value)) {
			return AuthorizationGrantType.DEVICE_CODE;
		}
		return new AuthorizationGrantType(value);
	}

}
