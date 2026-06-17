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

package org.jax.snack.oauth.biz.security.config;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.oauth.biz.security.OAuth2SecurityConstants;
import org.jax.snack.oauth.biz.security.PreAuthRestriction;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

/**
 * OAuth2 Token 定制器实现.
 * <p>
 * 实现作用域隔离模式，确保首次登录用户只能执行有限操作。
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2TokenCustomizerImpl implements OAuth2TokenCustomizer<JwtEncodingContext> {

	private static final int RESTRICTED_TOKEN_TTL_MINUTES = 5;

	private final UserDetailsService userDetailsService;

	private final List<PreAuthRestriction> restrictions;

	@Override
	public void customize(JwtEncodingContext context) {
		if (!isAccessTokenOrIdToken(context)) {
			return;
		}

		if (AuthorizationGrantType.REFRESH_TOKEN.equals(context.getAuthorizationGrantType())) {
			validateUserStateForRefresh(context.getPrincipal().getName());
			return;
		}

		if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
			Set<String> scopes = context.getAuthorizedScopes();
			Set<String> newScopes = (scopes != null) ? new HashSet<>(scopes) : new HashSet<>();
			newScopes.add("client");
			context.getClaims().claim("scope", newScopes);
			return;
		}

		Optional<String> activeScope = findActiveRestrictionScope(context.getPrincipal());
		if (activeScope.isPresent()) {
			context.getClaims().claim("scope", new HashSet<>(Set.of("openid", activeScope.get())));
			context.getClaims().claim("authorities", Collections.emptyList());
			context.getClaims().expiresAt(Instant.now().plus(RESTRICTED_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
		}
	}

	private void validateUserStateForRefresh(String username) {
		UserDetails freshUser;
		try {
			freshUser = this.userDetailsService.loadUserByUsername(username);
		}
		catch (UsernameNotFoundException ex) {
			log.warn("User not found during token refresh: {}", username);
			throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED));
		}
		boolean invalidState = !freshUser.isEnabled() || !freshUser.isAccountNonLocked()
				|| !freshUser.isAccountNonExpired();
		Set<String> restrictionAuthorities = this.restrictions.stream()
			.map(PreAuthRestriction::getAuthority)
			.collect(Collectors.toSet());
		boolean hasRestriction = freshUser.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.anyMatch(restrictionAuthorities::contains);
		if (invalidState || hasRestriction) {
			log.debug("User {} is in invalid state during token refresh, denying", username);
			throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED));
		}
	}

	/**
	 * 查找当前认证中激活的预授权限制 scope 名称（去掉 SCOPE_ 前缀）.
	 * @param principal 认证信息
	 * @return 限制 scope（如 pre_auth_reset、pre_auth_mfa），无则 empty
	 */
	private Optional<String> findActiveRestrictionScope(Authentication principal) {
		Set<String> authorities = principal.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toSet());
		return this.restrictions.stream()
			.filter((r) -> authorities.contains(r.getAuthority()))
			.findFirst()
			.map((r) -> r.getAuthority().substring(OAuth2SecurityConstants.SCOPE_PREFIX.length()));
	}

	private boolean isAccessTokenOrIdToken(JwtEncodingContext context) {
		String tokenType = context.getTokenType().getValue();
		return OAuth2TokenType.ACCESS_TOKEN.getValue().equals(tokenType)
				|| OidcParameterNames.ID_TOKEN.equals(tokenType);
	}

}
