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
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 会话状态检查过滤器.
 * <p>
 * 对已认证请求触发 AT 懒刷新：AT 有效期内无网络开销；AT 过期时通过 RT 触发 SAS 刷新， SAS 侧重新查 DB
 * 验证用户状态（密码过期、账号禁用、锁定等），状态异常则拒绝续发 token。 过滤器捕获到刷新异常时强制使 Session 失效并返回 401，让用户重新走登录流程。
 * 未认证请求（无 Session）直接放行，由 AuthenticationEntryPoint 处理。
 *
 * @author Jax Jiang
 */
@Slf4j
public class SessionStateCheckFilter extends OncePerRequestFilter {

	private static final String SESSION_EXPIRED_KEY = "oauth2.client.error.sessionExpired";

	private final OAuth2AuthorizedClientManager authorizedClientManager;

	private final String registrationId;

	private final String loginUrl;

	private final JsonMapper jsonMapper;

	private final SessionRefreshLock sessionRefreshLock;

	private final MessageSource messageSource;

	public SessionStateCheckFilter(OAuth2AuthorizedClientManager authorizedClientManager, String registrationId,
			String loginUrl, JsonMapper jsonMapper, SessionRefreshLock sessionRefreshLock, MessageSource messageSource) {
		this.authorizedClientManager = authorizedClientManager;
		this.registrationId = registrationId;
		this.loginUrl = loginUrl;
		this.jsonMapper = jsonMapper;
		this.sessionRefreshLock = sessionRefreshLock;
		this.messageSource = messageSource;
	}

	@Override
	@NullMarked protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (!(auth instanceof OAuth2AuthenticationToken)) {
			chain.doFilter(request, response);
			return;
		}

		HttpSession session = request.getSession(false);
		if (session == null) {
			chain.doFilter(request, response);
			return;
		}

		boolean locked = false;
		try {
			locked = this.sessionRefreshLock.tryLock(session.getId(), 3, TimeUnit.SECONDS);
			if (!locked) {
				chain.doFilter(request, response);
				return;
			}
			OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
				.withClientRegistrationId(this.registrationId)
				.principal(auth)
				.attributes((attrs) -> {
					attrs.put(HttpServletRequest.class.getName(), request);
					attrs.put(HttpServletResponse.class.getName(), response);
				})
				.build();
			OAuth2AuthorizedClient client = this.authorizedClientManager.authorize(authorizeRequest);
			if (client == null) {
				chain.doFilter(request, response);
				return;
			}
		}
		catch (OAuth2AuthorizationException ex) {
			log.debug("AT refresh denied ({}), forcing re-login", ex.getError().getErrorCode());
			forceLogoutAndReturn401(request, response);
			return;
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			chain.doFilter(request, response);
			return;
		}
		finally {
			if (locked) {
				this.sessionRefreshLock.unlock(session.getId());
			}
		}

		chain.doFilter(request, response);
	}

	private void forceLogoutAndReturn401(HttpServletRequest request, HttpServletResponse response) throws IOException {
		SecurityContextHolder.clearContext();
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		String msg = this.messageSource.getMessage(SESSION_EXPIRED_KEY, null, LocaleContextHolder.getLocale());
		Map<String, Object> body = Map.of("msg", msg, "data", Map.of("loginUrl", this.loginUrl));
		try (PrintWriter writer = response.getWriter()) {
			writer.write(this.jsonMapper.writeValueAsString(body));
			writer.flush();
		}
	}

}
