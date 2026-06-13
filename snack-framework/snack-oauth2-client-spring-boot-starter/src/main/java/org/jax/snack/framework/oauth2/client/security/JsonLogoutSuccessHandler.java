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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 登出成功处理器.
 * <p>
 * {@code endSessionEndpointUri} 未配置或与当前服务同 origin：返回 BFF OAuth2 授权入口地址。<br>
 * 跨 origin：返回 SAS 的 OIDC RP-Initiated Logout 地址（含 post_logout_redirect_uri 和
 * id_token_hint）， SAS 清除会话后按参数跳回 BFF 授权入口重走授权码流程。<br>
 * {@code endSessionEndpointUri} 通过 {@code snack.oauth2.client.end-session-endpoint-uri}
 * 配置， 与其他 SAS 端点地址格式一致。
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

	private final String loginUrl;

	private final String endSessionEndpointUri;

	private final JsonMapper jsonMapper;

	@Override
	public void onLogoutSuccess(@NonNull HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		try (PrintWriter writer = response.getWriter()) {
			writer.write(
					this.jsonMapper.writeValueAsString(Map.of("redirectUrl", oidcLogoutUrl(request, authentication))));
			writer.flush();
		}
	}

	private String oidcLogoutUrl(HttpServletRequest request, Authentication authentication) {
		if (!StringUtils.hasText(this.endSessionEndpointUri)) {
			return this.loginUrl;
		}
		String bffBase = UriComponentsBuilder.fromUriString(UrlUtils.buildFullRequestUrl(request))
			.replacePath(null)
			.replaceQuery(null)
			.fragment(null)
			.build()
			.toUriString();
		// SAS 与 BFF 同源时共用同一 HttpSession：
		// SecurityContextLogoutHandler.session.invalidate() 先于客户端跳转执行，
		// SAS 收到 /connect/logout 时 session 已销毁，无法找到关联 session 而返回 400。
		// 故同源时直接走重授权流程，无需 OIDC RP-Initiated Logout。
		String sasBase = UriComponentsBuilder.fromUriString(this.endSessionEndpointUri)
			.replacePath(null)
			.replaceQuery(null)
			.fragment(null)
			.build()
			.toUriString();
		if (bffBase.equals(sasBase)) {
			return this.loginUrl;
		}
		String postLogoutRedirectUri = bffBase + this.loginUrl;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(this.endSessionEndpointUri)
			.queryParam("post_logout_redirect_uri", postLogoutRedirectUri);
		String idToken = extractIdToken(authentication);
		if (idToken != null) {
			builder.queryParam("id_token_hint", idToken);
		}
		return builder.encode(StandardCharsets.UTF_8).build().toUriString();
	}

	private static String extractIdToken(Authentication authentication) {
		if (authentication instanceof OAuth2AuthenticationToken token
				&& token.getPrincipal() instanceof OidcUser oidcUser) {
			return oidcUser.getIdToken().getTokenValue();
		}
		return null;
	}

}
