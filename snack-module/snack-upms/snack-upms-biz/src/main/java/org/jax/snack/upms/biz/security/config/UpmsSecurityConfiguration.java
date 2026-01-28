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

package org.jax.snack.upms.biz.security.config;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;
import org.jax.snack.upms.biz.security.UpmsGrantedAuthoritiesMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.ObjectUtils;

/**
 * UPMS 安全配置定制器.
 *
 * @author Jax Jiang
 */
@Slf4j
@Configuration
public class UpmsSecurityConfiguration implements OAuth2ClientSecurityCustomizer {

	private final List<AuthorizationManager<RequestAuthorizationContext>> securityPolicies;

	private final UpmsGrantedAuthoritiesMapper grantedAuthoritiesMapper;

	public UpmsSecurityConfiguration(
			ObjectProvider<AuthorizationManager<RequestAuthorizationContext>> securityPoliciesProvider,
			UpmsGrantedAuthoritiesMapper grantedAuthoritiesMapper) {
		this.securityPolicies = securityPoliciesProvider.orderedStream().toList();
		this.grantedAuthoritiesMapper = grantedAuthoritiesMapper;

		log.info("Loaded {} security policies: {}", this.securityPolicies.size(), this.securityPolicies);
	}

	@Override
	public int getOrder() {
		return 100;
	}

	@Override
	public void customize(HttpSecurity http) {
	}

	@Override
	public void configureAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
		authorize.requestMatchers("/api/**").access((authentication, context) -> {
			if (ObjectUtils.isEmpty(context)) {
				return new AuthorizationDecision(false);
			}
			HttpServletRequest request = context.getRequest();
			if (ObjectUtils.isEmpty(request)) {
				return new AuthorizationDecision(false);
			}
			log.debug("UPMS Security checking path: {}. Registered policies count: {}", request.getRequestURI(),
					this.securityPolicies.size());
			for (AuthorizationManager<RequestAuthorizationContext> policy : this.securityPolicies) {
				AuthorizationResult result = policy.authorize(authentication, context);
				log.debug("Policy {} result: {}", policy.getClass().getSimpleName(),
						((result != null) ? result.isGranted() : "null"));
				if (result != null && !result.isGranted()) {
					return result;
				}
			}
			return new AuthorizationDecision(true);
		});
	}

	@Override
	public GrantedAuthoritiesMapper authoritiesMapper() {
		return this.grantedAuthoritiesMapper;
	}

}
