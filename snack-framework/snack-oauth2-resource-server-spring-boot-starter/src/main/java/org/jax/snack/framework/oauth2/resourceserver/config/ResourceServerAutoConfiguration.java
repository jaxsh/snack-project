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

package org.jax.snack.framework.oauth2.resourceserver.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * Resource Server 自动配置.
 * <p>
 * 当应用作为纯 Resource Server (没有 OAuth2 Client) 时生效.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
@ConditionalOnClass(JwtAuthenticationToken.class)
@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
@EnableConfigurationProperties(ResourceServerProperties.class)
public class ResourceServerAutoConfiguration {

	/**
	 * 资源服务器安全过滤链.
	 * @param http HttpSecurity
	 * @param properties Resource Server 配置属性
	 * @return SecurityFilterChain
	 */
	@Bean
	@Order(3)
	@ConditionalOnMissingBean(name = "oauth2ClientSecurityFilterChain")
	public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http,
			ResourceServerProperties properties) {

		http.authorizeHttpRequests((authorize) -> {
			for (String path : properties.getPermitAllPaths()) {
				authorize.requestMatchers(path).permitAll();
			}
			authorize.anyRequest().authenticated();
		})
			.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
			.csrf((csrf) -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));

		return http.build();
	}

	/**
	 * JWT 解码器.
	 * <p>
	 * 添加 Audience (aud) 验证, 确保 Token 的 aud 包含当前服务名.
	 * @param jwkSetUri JWK Set URI
	 * @param applicationName 应用名称 (用于 aud 验证)
	 * @param properties Resource Server 配置属性
	 * @return JwtDecoder
	 */
	@Bean
	@ConditionalOnMissingBean
	public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
			@Value("${spring.application.name:}") String applicationName, ResourceServerProperties properties) {

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

		if (properties.isValidateAudience() && StringUtils.hasText(applicationName)) {
			OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>("aud",
					(aud) -> aud != null && aud.contains(applicationName));

			OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
					JwtValidators.createDefault(), audienceValidator);

			decoder.setJwtValidator(combinedValidator);
		}

		return decoder;
	}

}
