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

package org.jax.snack.oauth.biz.security.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

/**
 * 认证成功 API 响应处理器.
 * <p>
 * 该处理器将认证成功的响应统一为标准的 API 响应格式 (JSON), 并提供 OAuth2 授权重定向地址.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class JsonAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

	private final JsonMapper jsonMapper;

	private final RequestCache requestCache = new HttpSessionRequestCache();

	@Override
	@NullMarked public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		SavedRequest savedRequest = this.requestCache.getRequest(request, response);
		String redirectUrl = (savedRequest != null) ? savedRequest.getRedirectUrl() : null;

		ApiResponse<Map<String, String>> apiResponse = ApiResponse
			.success(Collections.singletonMap("redirectUrl", redirectUrl));

		try (PrintWriter writer = response.getWriter()) {
			writer.write(this.jsonMapper.writeValueAsString(apiResponse));
			writer.flush();
		}
	}

}
