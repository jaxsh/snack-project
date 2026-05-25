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

package org.jax.snack.upms.biz.config;

import org.jax.snack.framework.oauth2.client.config.OAuth2ClientProperties;
import org.jax.snack.upms.biz.client.OAuth2UserClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * OAuth2 用户客户端配置.
 *
 * @author Jax Jiang
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2UserClientConfiguration {

	@Bean
	OAuth2UserClient oauth2UserClient(RestClient.Builder builder, OAuth2AuthorizedClientManager manager,
			OAuth2ClientProperties properties) {
		String baseUrl = properties.getServerUrl();
		if (baseUrl == null || baseUrl.isEmpty()) {
			throw new IllegalArgumentException("OAuth2 server URL must be configured");
		}

		OAuth2ClientHttpRequestInterceptor interceptor = new OAuth2ClientHttpRequestInterceptor(manager);
		interceptor.setClientRegistrationIdResolver((request) -> "snack-upms-service");
		RestClient restClient = builder.baseUrl(baseUrl).requestInterceptor(interceptor).build();
		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
			.build()
			.createClient(OAuth2UserClient.class);
	}

}
