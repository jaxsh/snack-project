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

package org.jax.snack.upms.biz.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.oauth2.client.spi.LoginAuditHandler;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Spring Security 认证事件监听器.
 * <p>
 * 监听登录成功/失败事件, 触发审计逻辑.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpmsAuthenticationEvents {

	private final LoginAuditHandler loginAuditHandler;

	private final ObjectProvider<HttpServletRequest> requestProvider;

	/**
	 * 处理登录成功事件.
	 * @param event 交互式认证成功事件
	 */
	@EventListener
	public void onSuccess(InteractiveAuthenticationSuccessEvent event) {
		Authentication authentication = event.getAuthentication();
		if (authentication instanceof OAuth2AuthenticationToken) {
			String username = extractUsername(authentication.getPrincipal());
			if (username != null) {
				HttpServletRequest request = this.requestProvider.getIfAvailable();
				this.loginAuditHandler.recordLoginSuccess(username, request);
			}
		}
	}

	/**
	 * 处理登录失败事件.
	 * @param event 认证失败事件
	 */
	@EventListener
	public void onFailure(AbstractAuthenticationFailureEvent event) {
		Object principal = event.getAuthentication().getPrincipal();
		if (principal instanceof String username) {
			HttpServletRequest request = this.requestProvider.getIfAvailable();
			String reason = resolveFailureReason(event.getException());
			this.loginAuditHandler.recordLoginFailure(username, reason, request);
		}
	}

	/**
	 * 将认证异常映射为语义化的英文原因码.
	 * @param ex 认证异常
	 * @return 原因码 (英文)
	 */
	private String resolveFailureReason(AuthenticationException ex) {
		if (ex instanceof BadCredentialsException || ex instanceof UsernameNotFoundException) {
			return "INVALID_CREDENTIALS";
		}
		if (ex instanceof LockedException) {
			return "ACCOUNT_LOCKED";
		}
		if (ex instanceof DisabledException) {
			return "ACCOUNT_DISABLED";
		}
		if (ex instanceof AccountExpiredException) {
			return "ACCOUNT_EXPIRED";
		}
		if (ex instanceof CredentialsExpiredException) {
			return "PASSWORD_EXPIRED";
		}
		return "AUTHENTICATION_FAILED";
	}

	/**
	 * 从 Principal 中提取用户名.
	 * @param principal 认证主体
	 * @return 用户名, 无法识别则返回 null
	 */
	private String extractUsername(Object principal) {
		if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}
		if (principal instanceof OAuth2User oauth2User) {
			return oauth2User.getName();
		}
		return null;
	}

}
