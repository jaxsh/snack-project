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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jspecify.annotations.NullMarked;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

/**
 * OIDC Scope 权限映射器.
 * <p>
 * 将 OIDC ID Token 中的 scope claim 映射为 SCOPE_ 前缀的权限.
 *
 * @author Jax Jiang
 */
public class OidcScopeGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

	@NullMarked
	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> mapped = new HashSet<>(authorities);
		for (GrantedAuthority authority : authorities) {
			if (authority instanceof OidcUserAuthority oidcAuth) {
				Object scope = oidcAuth.getIdToken().getClaim("scope");
				if (scope instanceof Collection<?> scopes) {
					scopes.forEach((s) -> mapped.add(new SimpleGrantedAuthority("SCOPE_" + s)));
				}
			}
		}
		return mapped;
	}

}
