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

package org.jax.snack.oauth.biz.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.oauth.api.dto.MfaVerifyDTO;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.service.OAuthUserService;
import org.jax.snack.oauth.biz.security.OAuth2SecurityConstants;
import org.jax.snack.oauth.biz.security.mfa.MfaProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 用户自助服务控制器.
 * <p>
 * 处理用户自助改密及 MFA 登录验证，使用 session cookie 认证.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/oauth2/account")
@RequiredArgsConstructor
public class OAuthUserController {

	private final OAuthUserService oAuthUserService;

	private final UserDetailsService userDetailsService;

	private final ObjectProvider<MfaProvider> mfaProviderProvider;

	private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

	private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

	/**
	 * 修改密码.
	 * <p>
	 * 受限用户（初始密码或过期密码）改密后升级 session 并返回 redirectUrl.
	 * @param body 请求体
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return 含 redirectUrl 的 map
	 */
	@PostMapping("/change-password")
	@PreAuthorize("hasAuthority('" + OAuth2SecurityConstants.SCOPE_PREFIX + OAuth2SecurityConstants.PRE_AUTH_RESET_SCOPE
			+ "')")
	public Map<String, String> changePassword(@RequestBody @Validated OAuthUserDTO body, HttpServletRequest request,
			HttpServletResponse response) {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		body.setInitialPassword(YesNoStatus.NO.getCode());
		this.oAuthUserService.update(username, body);
		return Map.of("redirectUrl", upgradeSessionAndGetRedirectUrl(request, response));
	}

	/**
	 * MFA 登录验证.
	 * <p>
	 * 校验 6 位动态码，通过后升级 session 并返回 redirectUrl.
	 * @param body 请求体
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @return 含 redirectUrl 的 map
	 */
	@PostMapping("/verify-mfa")
	@PreAuthorize("hasAuthority('" + OAuth2SecurityConstants.SCOPE_PREFIX + OAuth2SecurityConstants.PRE_AUTH_MFA_SCOPE
			+ "')")
	public Map<String, String> verifyMfa(@RequestBody @Validated MfaVerifyDTO body, HttpServletRequest request,
			HttpServletResponse response) {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		MfaProvider provider = this.mfaProviderProvider.getIfAvailable();
		if (provider == null || !provider.verify(username, body.getCode())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "MFA code");
		}
		return Map.of("redirectUrl", upgradeSessionAndGetRedirectUrl(request, response));
	}

	private String upgradeSessionAndGetRedirectUrl(HttpServletRequest request, HttpServletResponse response) {
		String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
		UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(OAuth2SecurityConstants.ROLE_USER));
		authorities.add(FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY));
		UsernamePasswordAuthenticationToken upgraded = UsernamePasswordAuthenticationToken.authenticated(userDetails,
				null, authorities);
		SecurityContext ctx = SecurityContextHolder.createEmptyContext();
		ctx.setAuthentication(upgraded);
		SecurityContextHolder.setContext(ctx);
		this.contextRepository.saveContext(ctx, request, response);
		SavedRequest savedRequest = this.requestCache.getRequest(request, response);
		if (savedRequest == null) {
			throw new IllegalStateException("No saved OAuth2 authorization request found in session.");
		}
		return savedRequest.getRedirectUrl();
	}

}
