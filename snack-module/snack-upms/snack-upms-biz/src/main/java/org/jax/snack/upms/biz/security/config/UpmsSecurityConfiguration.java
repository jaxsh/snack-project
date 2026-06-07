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

import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;
import org.jax.snack.upms.biz.security.UpmsGrantedAuthoritiesMapper;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

/**
 * UPMS 安全配置定制器.
 *
 * @author Jax Jiang
 */
@Configuration
public class UpmsSecurityConfiguration implements OAuth2ClientSecurityCustomizer {

	private final UpmsGrantedAuthoritiesMapper grantedAuthoritiesMapper;

	public UpmsSecurityConfiguration(UpmsGrantedAuthoritiesMapper grantedAuthoritiesMapper) {
		this.grantedAuthoritiesMapper = grantedAuthoritiesMapper;
	}

	@Override
	public int getOrder() {
		return -100;
	}

	@Override
	public void customize(HttpSecurity http) {
	}

	@Override
	public void configureAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
		authorize.requestMatchers(HttpMethod.PUT, "/api/upms/users/password").authenticated();
		authorize.requestMatchers(HttpMethod.GET, "/api/upms/users/info", "/api/upms/users/resources").authenticated();
	}

	@Override
	public GrantedAuthoritiesMapper authoritiesMapper() {
		return this.grantedAuthoritiesMapper;
	}

}
