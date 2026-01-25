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

import java.util.List;

import org.jax.snack.oauth.biz.security.OAuth2SecurityPolicy;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * 默认安全配置.
 * <p>
 * 处理 Authorization Server 端点以外的请求, 包括表单登录.
 * <p>
 * 仅在独立运行模式下生效 (无 OAuth2 Client 安全链时).
 *
 * @author Jax Jiang
 */
@Configuration
@EnableWebSecurity
@ConditionalOnMissingBean(name = "oauth2ClientSecurityFilterChain")
public class DefaultSecurityConfig {

	/**
	 * 默认安全过滤链.
	 * <p>
	 * 配置表单登录和基本的授权规则.
	 * @param http HttpSecurity
	 * @param securityPoliciesProvider AuthorizationManager provider
	 * @return SecurityFilterChain
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
			ObjectProvider<AuthorizationManager<RequestAuthorizationContext>> securityPoliciesProvider) {
		List<AuthorizationManager<RequestAuthorizationContext>> securityPolicies = securityPoliciesProvider
			.orderedStream()
			.toList();

		http.authorizeHttpRequests((authorize) -> {
			authorize.requestMatchers("/error", "/actuator/health").permitAll();
			OAuth2SecurityPolicy.configureAuthorization(authorize, securityPolicies);
			authorize.anyRequest().authenticated();
		})
			.formLogin(Customizer.withDefaults())
			.csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
			.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}

}
