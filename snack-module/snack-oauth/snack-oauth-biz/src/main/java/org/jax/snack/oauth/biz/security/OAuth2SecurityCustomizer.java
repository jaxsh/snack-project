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

import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

/**
 * OAuth2 模块安全定制器.
 * <p>
 * 提供强制修改密码时的路径访问限制逻辑.
 *
 * @author Jax Jiang
 */
@Component
@ConditionalOnClass(OAuth2ClientSecurityCustomizer.class)
public class OAuth2SecurityCustomizer implements OAuth2ClientSecurityCustomizer {

	@Override
	public void customize(HttpSecurity http) {
	}

	@Override
	public void configureAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
		OAuth2SecurityPolicy.configureAuthorization(authorize);
	}

}
