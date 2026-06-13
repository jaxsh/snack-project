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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 支持记住我的登录成功处理器装饰器.
 * <p>
 * 在 OAuth2 回调时读取 {@code x-remember-me} 意图 cookie：若存在则将 session 延长至 30 天并写入持久化 session
 * cookie，然后立即删除意图 cookie 以防重放，最后委托给被装饰的处理器执行重定向.
 *
 * @author Jax Jiang
 */
public class RememberMeAwareSuccessHandler implements AuthenticationSuccessHandler {

	private static final String REMEMBER_ME_COOKIE = "x-remember-me";

	private static final int REMEMBER_ME_MAX_AGE = 30 * 24 * 3600;

	private final AuthenticationSuccessHandler delegate;

	/**
	 * 构造函数.
	 * @param delegate 被装饰的认证成功处理器
	 */
	public RememberMeAwareSuccessHandler(AuthenticationSuccessHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void onAuthenticationSuccess(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Authentication authentication) throws IOException, ServletException {
		if (hasRememberMeCookie(request)) {
			extendSession(request, response);
			deleteRememberMeCookie(response);
		}
		this.delegate.onAuthenticationSuccess(request, response, authentication);
	}

	private boolean hasRememberMeCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return false;
		}
		for (Cookie c : cookies) {
			if (REMEMBER_ME_COOKIE.equals(c.getName()) && "1".equals(c.getValue())) {
				return true;
			}
		}
		return false;
	}

	private void extendSession(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return;
		}
		session.setMaxInactiveInterval(REMEMBER_ME_MAX_AGE);
		String cookieName = request.getServletContext().getSessionCookieConfig().getName();
		if (cookieName == null) {
			cookieName = "JSESSIONID";
		}
		Cookie sessionCookie = new Cookie(cookieName, session.getId());
		sessionCookie.setMaxAge(REMEMBER_ME_MAX_AGE);
		sessionCookie.setHttpOnly(true);
		sessionCookie.setPath("/");
		sessionCookie.setSecure(request.isSecure());
		response.addCookie(sessionCookie);
	}

	private void deleteRememberMeCookie(HttpServletResponse response) {
		Cookie del = new Cookie(REMEMBER_ME_COOKIE, "");
		del.setMaxAge(0);
		del.setPath("/");
		response.addCookie(del);
	}

}
