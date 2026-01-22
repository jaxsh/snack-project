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

import java.util.ArrayList;
import java.util.List;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * OAuth2 Authorization Server 配置.
 *
 * @author Jax Jiang
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OAuth2AuthorizationServerConfigurer.class)
@RequiredArgsConstructor
public class AuthorizationServerConfig {

	private final RsaKeyProperties rsaKeyProperties;

	/**
	 * 授权服务器安全过滤链.
	 * @param http HttpSecurity
	 * @return SecurityFilterChain
	 */
	@Bean
	@Order(1)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

		http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
			.with(authorizationServerConfigurer,
					(authorizationServer) -> authorizationServer.oidc(Customizer.withDefaults()))
			.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
			.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
					new LoginUrlAuthenticationEntryPoint("/login"), new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
			.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(Customizer.withDefaults()));

		return http.build();
	}

	/**
	 * OAuth2 授权服务.
	 * <p>
	 * 使用 JDBC 持久化 Token.
	 * @param jdbcTemplate JdbcTemplate
	 * @param registeredClientRepository RegisteredClientRepository
	 * @return JdbcOAuth2AuthorizationService
	 */
	@Bean
	public JdbcOAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate,
			RegisteredClientRepository registeredClientRepository) {
		return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
	}

	/**
	 * JWK 源.
	 * <p>
	 * 支持多密钥, 用于密钥轮换. 所有密钥都可用于验证, 仅 active 密钥用于签发.
	 * @return JWKSource
	 */
	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		List<JWK> jwkList = new ArrayList<>();

		for (RsaKeyProperties.KeyEntry keyEntry : this.rsaKeyProperties.getKeys()) {
			RSAKey.Builder builder = new RSAKey.Builder(keyEntry.getPublicKey()).keyID(keyEntry.getKeyId());
			if (keyEntry.getPrivateKey() != null) {
				builder.privateKey(keyEntry.getPrivateKey());
			}
			jwkList.add(builder.build());
		}

		JWKSet jwkSet = new JWKSet(jwkList);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	/**
	 * JWT 解码器.
	 * @param jwkSource JWKSource
	 * @return JwtDecoder
	 */
	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	/**
	 * 授权服务器设置.
	 * @return AuthorizationServerSettings
	 */
	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().build();
	}

	/**
	 * 密码编码器.
	 * <p>
	 * 使用 DelegatingPasswordEncoder 支持多种编码格式.
	 * @return PasswordEncoder
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
