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

import java.io.IOException;

import org.jspecify.annotations.NullMarked;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

/**
 * OAuth2 令牌中继拦截器.
 * <p>
 * 在请求头中自动注入当前登录用户的 Bearer Token. 如果 Access Token 已过期, 将尝试使用 Refresh Token 自动刷新.
 *
 * @author Jax Jiang
 */
public class TokenRelayClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	private final OAuth2AuthorizedClientManager authorizedClientManager;

	private final String registrationId;

	public TokenRelayClientHttpRequestInterceptor(OAuth2AuthorizedClientManager authorizedClientManager,
			String registrationId) {
		this.authorizedClientManager = authorizedClientManager;
		this.registrationId = registrationId;
	}

	@Override
	@NullMarked public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null) {
			OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId(this.registrationId)
				.principal(authentication)
				.build();

			OAuth2AuthorizedClient authorizedClient = this.authorizedClientManager.authorize(authorizeRequest);

			if (authorizedClient != null) {
				String tokenValue = authorizedClient.getAccessToken().getTokenValue();
				request.getHeaders().setBearerAuth(tokenValue);
			}
		}

		return execution.execute(request, body);
	}

}
