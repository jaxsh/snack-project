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

import java.util.List;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/**
 * 基于用户名比较的 SessionRegistry.
 * <p>
 * {@link SessionRegistryImpl} 使用 principal 对象的 {@code equals()} 查找会话。 对于 OIDC 登录，每次登录颁发的
 * ID Token 含不同 nonce，导致 {@code OidcUser.equals()} 返回 false，
 * {@link org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy}
 * 无法找到已有会话，并发限制形同虚设.
 * <p>
 * 本实现继承 {@link SessionRegistryImpl}，覆盖 {@link #getAllSessions} 改为按用户名（principal name）匹配.
 * 继承而非组合确保 {@code HttpSessionDestroyedEvent} 能正确触发，旧会话可被及时清理.
 *
 * @author Jax Jiang
 */
public class UsernameBasedSessionRegistry extends SessionRegistryImpl {

	@Override
	public List<SessionInformation> getAllSessions(Object principal, boolean includeExpiredSessions) {
		String username = extractUsername(principal);
		return getAllPrincipals().stream()
			.filter((p) -> username.equals(extractUsername(p)))
			.flatMap((p) -> super.getAllSessions(p, includeExpiredSessions).stream())
			.toList();
	}

	private static String extractUsername(Object principal) {
		if (principal instanceof UserDetails u) {
			return u.getUsername();
		}
		if (principal instanceof OAuth2AuthenticatedPrincipal o) {
			return o.getName();
		}
		return String.valueOf(principal);
	}

}
