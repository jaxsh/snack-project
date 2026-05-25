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
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * 未认证 API 响应处理器.
 * <p>
 * 针对 /api/** 路径的未认证请求返回标准 JSON 格式的 401 响应，而不是 302 重定向.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class BizAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper jsonMapper;

	private final String loginUrl;

	@Override
	@NullMarked public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		ApiResponse<Map<String, String>> apiResponse = ApiResponse.error(ErrorCode.PERMISSION_DENIED,
				authException.getLocalizedMessage(), Map.of("loginUrl", this.loginUrl));

		try (PrintWriter writer = response.getWriter()) {
			writer.write(this.jsonMapper.writeValueAsString(apiResponse));
			writer.flush();
		}
	}

}
