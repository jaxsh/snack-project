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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * 登出成功处理器.
 * <p>
 * 返回 JSON 响应，包含前端重新发起 OAuth2 授权码流程所需的跳转地址.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class JsonLogoutSuccessHandler implements LogoutSuccessHandler {

	private final String loginUrl;

	@Override
	public void onLogoutSuccess(@NonNull HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String body = "{\"redirectUrl\":\"" + this.loginUrl + "\"}";
		try (PrintWriter writer = response.getWriter()) {
			writer.write(body);
			writer.flush();
		}
	}

}
