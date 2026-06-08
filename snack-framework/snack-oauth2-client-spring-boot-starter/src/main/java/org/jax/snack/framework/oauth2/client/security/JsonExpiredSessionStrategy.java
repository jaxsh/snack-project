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

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.web.session.SessionInformationExpiredEvent;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;

/**
 * 并发 Session 过期时返回 JSON 响应.
 * <p>
 * 响应包含 {@code msg} 字段，由 {@link MessageSource} 按请求 locale 解析，支持国际化.
 *
 * @author Jax Jiang
 */
@RequiredArgsConstructor
public class JsonExpiredSessionStrategy implements SessionInformationExpiredStrategy {

	private static final String CONCURRENT_SESSION_KEY = "oauth2.client.error.concurrentSession";

	private final String loginUrl;

	private final JsonMapper jsonMapper;

	private final MessageSource messageSource;

	@Override
	public void onExpiredSessionDetected(SessionInformationExpiredEvent event) throws IOException {
		HttpServletResponse response = event.getResponse();
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String msg = this.messageSource.getMessage(CONCURRENT_SESSION_KEY, null, LocaleContextHolder.getLocale());
		Map<String, Object> body = Map.of("msg", msg, "data", Map.of("loginUrl", this.loginUrl));
		try (PrintWriter writer = response.getWriter()) {
			writer.write(this.jsonMapper.writeValueAsString(body));
			writer.flush();
		}
	}

}
