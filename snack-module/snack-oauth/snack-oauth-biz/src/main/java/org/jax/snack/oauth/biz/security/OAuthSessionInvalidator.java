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

package org.jax.snack.oauth.biz.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionCreatedEvent;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;
import org.springframework.stereotype.Component;

/**
 * SAS 侧 HttpSession 失效器.
 * <p>
 * 监听 session 生命周期事件，维护 sessionId→HttpSession 映射，强制踢人时直接 invalidate 目标用户的所有 session，确保 SAS
 * 侧不再静默重新授权.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
public class OAuthSessionInvalidator {

	private final ConcurrentMap<String, HttpSession> sessions = new ConcurrentHashMap<>();

	/**
	 * Session 创建时加入映射.
	 * @param event HttpSessionCreatedEvent
	 */
	@EventListener
	public void onSessionCreated(HttpSessionCreatedEvent event) {
		this.sessions.put(event.getSession().getId(), event.getSession());
	}

	/**
	 * Session 销毁时从映射移除.
	 * @param event HttpSessionDestroyedEvent
	 */
	@EventListener
	public void onSessionDestroyed(HttpSessionDestroyedEvent event) {
		this.sessions.remove(event.getSession().getId());
	}

	/**
	 * 强制失效指定用户的所有活跃 session.
	 * @param username 用户名
	 */
	public void invalidateByUsername(String username) {
		this.sessions.forEach((sessionId, session) -> {
			if (matchesUsername(session, username)) {
				try {
					session.invalidate();
				}
				catch (IllegalStateException ex) {
					log.debug("Session already invalidated: {}", sessionId);
				}
			}
		});
		this.sessions.forEach((sessionId, session) -> {
			if (matchesUsername(session, username)) {
				try {
					session.invalidate();
				}
				catch (IllegalStateException ex) {
					log.debug("Session already invalidated on second pass: {}", sessionId);
				}
			}
		});
	}

	private static boolean matchesUsername(HttpSession session, String username) {
		try {
			Object attr = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
			if (attr instanceof SecurityContext ctx && ctx.getAuthentication() != null) {
				Authentication auth = ctx.getAuthentication();
				if (!(auth.getPrincipal() instanceof UserDetails)) {
					return false;
				}
				return username.equals(auth.getName());
			}
		}
		catch (IllegalStateException ex) {
			return false;
		}
		return false;
	}

}
