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

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jax.snack.oauth.biz.security.config.SecurityProperties;
import org.jspecify.annotations.NonNull;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 预授权限制过滤器.
 *
 * @author Jax Jiang
 */
public class PreAuthRestrictionFilter extends OncePerRequestFilter {

	private final List<PreAuthRestriction> restrictions;

	private final SecurityProperties securityProperties;

	private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

	public PreAuthRestrictionFilter(List<PreAuthRestriction> restrictions, SecurityProperties securityProperties) {
		this.restrictions = restrictions;
		this.securityProperties = securityProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !"/oauth2/authorize".equals(request.getServletPath());
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain chain) throws ServletException, IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
			chain.doFilter(request, response);
			return;
		}
		Set<String> userAuthorities = auth.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toSet());
		for (PreAuthRestriction restriction : this.restrictions) {
			if (userAuthorities.contains(restriction.getAuthority())) {
				restriction.onApplied(auth.getName());
				this.requestCache.saveRequest(request, response);
				response.sendRedirect(this.securityProperties.getFrontendBaseUrl() + restriction.getPagePath());
				return;
			}
		}
		chain.doFilter(request, response);
	}

}
