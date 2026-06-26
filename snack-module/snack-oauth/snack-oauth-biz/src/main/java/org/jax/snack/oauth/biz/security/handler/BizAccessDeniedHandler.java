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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * 访问拒绝 API 响应处理器.
 * <p>
 * 已认证用户访问无权限资源时, 返回标准 JSON 格式的 403 响应, 而不是 Spring Security 默认的 HTML 错误页.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class BizAccessDeniedHandler implements AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	@Override
	@NullMarked public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());

		String msg = SpringSecurityMessageSource.getAccessor()
				.getMessage("AbstractAccessDecisionManager.accessDenied", "Access Denied");
		ApiResponse<Void> apiResponse = ApiResponse.error(ErrorCode.PERMISSION_DENIED, msg);

		try (PrintWriter writer = response.getWriter()) {
			writer.write(this.jsonMapper.writeValueAsString(apiResponse));
			writer.flush();
		}
	}

}
