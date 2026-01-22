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

package org.jax.snack.framework.oauth2.client.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.oauth2.client.spi.OAuth2TokenClient;
import org.jspecify.annotations.NonNull;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;

/**
 * Token 吊销退出处理器.
 * <p>
 * 退出时调用 Authorization Server 的 /oauth2/revoke 端点吊销 Token.
 *
 * @author Jax Jiang
 */
@Slf4j
public class RevokeTokenLogoutHandler implements LogoutHandler {

	private static final String ACCESS_TOKEN_HINT = "access_token";

	private static final String REFRESH_TOKEN_HINT = "refresh_token";

	private final OAuth2AuthorizedClientRepository authorizedClientRepository;

	private final OAuth2TokenClient tokenClient;

	private final String registrationId;

	/**
	 * 构造函数.
	 * @param authorizedClientRepository OAuth2 授权客户端仓储
	 * @param tokenClient Token 操作客户端
	 * @param registrationId Client 注册 ID
	 */
	public RevokeTokenLogoutHandler(OAuth2AuthorizedClientRepository authorizedClientRepository,
			OAuth2TokenClient tokenClient, String registrationId) {
		this.authorizedClientRepository = authorizedClientRepository;
		this.tokenClient = tokenClient;
		this.registrationId = registrationId;
	}

	@Override
	public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			Authentication authentication) {
		if (authentication == null) {
			return;
		}

		OAuth2AuthorizedClient authorizedClient = this.authorizedClientRepository
			.loadAuthorizedClient(this.registrationId, authentication, request);

		if (authorizedClient == null) {
			log.debug("No authorized client found for registration: {}", this.registrationId);
			return;
		}

		revokeTokens(authorizedClient);

		this.authorizedClientRepository.removeAuthorizedClient(this.registrationId, authentication, request, response);
	}

	/**
	 * 吊销 Token.
	 * @param authorizedClient 授权客户端
	 */
	private void revokeTokens(OAuth2AuthorizedClient authorizedClient) {
		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		if (accessToken != null) {
			revokeToken(accessToken.getTokenValue(), ACCESS_TOKEN_HINT);
		}

		OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();
		if (refreshToken != null) {
			revokeToken(refreshToken.getTokenValue(), REFRESH_TOKEN_HINT);
		}
	}

	/**
	 * 吊销单个 Token.
	 * @param tokenValue Token 值
	 * @param tokenTypeHint Token 类型提示
	 */
	private void revokeToken(String tokenValue, String tokenTypeHint) {
		try {
			MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
			formData.add("token", tokenValue);
			formData.add("token_type_hint", tokenTypeHint);
			this.tokenClient.revokeToken(formData);
			log.debug("Revoked {} for client: {}", tokenTypeHint, this.registrationId);
		}
		catch (RestClientException ex) {
			log.warn("Failed to revoke {}: {}", tokenTypeHint, ex.getMessage());
		}
	}

}
