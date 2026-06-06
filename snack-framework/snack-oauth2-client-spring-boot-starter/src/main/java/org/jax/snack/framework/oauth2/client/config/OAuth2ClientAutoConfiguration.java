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

package org.jax.snack.framework.oauth2.client.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jax.snack.framework.oauth2.client.security.AuditLogoutHandler;
import org.jax.snack.framework.oauth2.client.security.JsonLogoutSuccessHandler;
import org.jax.snack.framework.oauth2.client.security.OidcScopeGrantedAuthoritiesMapper;
import org.jax.snack.framework.oauth2.client.security.RevokeTokenLogoutHandler;
import org.jax.snack.framework.oauth2.client.security.SessionStateCheckFilter;
import org.jax.snack.framework.oauth2.client.spi.LoginAuditHandler;
import org.jax.snack.framework.oauth2.client.spi.OAuth2ClientSecurityCustomizer;
import org.jax.snack.framework.oauth2.client.spi.OAuth2TokenClient;
import org.jspecify.annotations.NullMarked;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * OAuth2 Client 自动配置.
 * <p>
 * 当引入 spring-boot-starter-oauth2-client 时自动启用.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
@ConditionalOnClass(OAuth2AuthorizedClientManager.class)
@ConditionalOnProperty(name = "snack.oauth2.client.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2ClientAutoConfiguration {

	/**
	 * 注入 spring.application.name 到 OAuth2ClientProperties.
	 * @param applicationName 应用名称
	 * @return BeanPostProcessor
	 */
	@Bean
	public static BeanPostProcessor oauth2ClientPropertiesPostProcessor(
			@Value("${spring.application.name:unknown}") String applicationName) {
		return new BeanPostProcessor() {
			@Override
			@NullMarked public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof OAuth2ClientProperties properties) {
					if (properties.getApplicationName() == null) {
						properties.setApplicationName(applicationName);
					}
				}
				return bean;
			}
		};
	}

	/**
	 * OAuth2 客户端安全过滤链.
	 * @param http HttpSecurity
	 * @param properties OAuth2 Client 配置属性
	 * @param customizers 定制器列表
	 * @param logoutHandlers 退出处理器列表
	 * @param defaultMapperProvider 默认权限映射器提供者
	 * @param authorizedClientManager OAuth2 授权客户端管理器
	 * @param jsonMapper JSON 序列化器
	 * @return SecurityFilterChain
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain oauth2ClientSecurityFilterChain(HttpSecurity http, OAuth2ClientProperties properties,
			ObjectProvider<OAuth2ClientSecurityCustomizer> customizers, ObjectProvider<LogoutHandler> logoutHandlers,
			@Qualifier("oidcScopeGrantedAuthoritiesMapper") ObjectProvider<GrantedAuthoritiesMapper> defaultMapperProvider,
			OAuth2AuthorizedClientManager authorizedClientManager, JsonMapper jsonMapper) {

		GrantedAuthoritiesMapper defaultMapper = defaultMapperProvider
			.getIfAvailable(OidcScopeGrantedAuthoritiesMapper::new);

		CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		CsrfTokenRequestAttributeHandler csrfTokenRequestAttributeHandler = new CsrfTokenRequestAttributeHandler();
		csrfTokenRequestAttributeHandler.setCsrfRequestAttributeName(null);

		List<OAuth2ClientSecurityCustomizer> sortedCustomizers = customizers.orderedStream().toList();

		GrantedAuthoritiesMapper authoritiesMapper = sortedCustomizers.stream()
			.map(OAuth2ClientSecurityCustomizer::authoritiesMapper)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(defaultMapper);

		http.authorizeHttpRequests((authorize) -> {
			for (String path : properties.getPermitAllPaths()) {
				authorize.requestMatchers(path).permitAll();
			}
			authorize.requestMatchers("/logout").permitAll();
			authorize.requestMatchers("/login/oauth2/**").permitAll();
			for (OAuth2ClientSecurityCustomizer customizer : sortedCustomizers) {
				customizer.configureAuthorization(authorize);
			}
			authorize.anyRequest().authenticated();
		})
			.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
			.csrf((csrf) -> csrf.csrfTokenRepository(cookieCsrfTokenRepository)
				.csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
				.ignoringRequestMatchers("/login", "/logout"))
			.cors(Customizer.withDefaults())
			.formLogin(Customizer.withDefaults())
			.oauth2Login((oauth2Login) -> {
				oauth2Login.userInfoEndpoint((userInfo) -> userInfo.userAuthoritiesMapper(authoritiesMapper));
				oauth2Login
					.successHandler(new SimpleUrlAuthenticationSuccessHandler(properties.getDefaultSuccessUrl()));
			})
			.oauth2Client(Customizer.withDefaults());

		for (OAuth2ClientSecurityCustomizer customizer : sortedCustomizers) {
			customizer.customize(http);
		}

		List<LogoutHandler> allLogoutHandlers = new ArrayList<>();
		for (OAuth2ClientSecurityCustomizer customizer : sortedCustomizers) {
			allLogoutHandlers.addAll(customizer.logoutHandlers());
		}
		logoutHandlers.orderedStream().forEach(allLogoutHandlers::add);
		allLogoutHandlers.add(defaultLogoutHandler(cookieCsrfTokenRepository));

		String loginUrl = "/oauth2/authorization/" + properties.getDefaultRegistrationId();
		http.logout((logout) -> {
			for (LogoutHandler handler : allLogoutHandlers) {
				logout.addLogoutHandler(handler);
			}
			logout.logoutSuccessHandler(
					new JsonLogoutSuccessHandler(loginUrl, properties.getEndSessionEndpointUri(), jsonMapper));
		});

		http.addFilterAfter(
				new SessionStateCheckFilter(authorizedClientManager, properties.getDefaultRegistrationId(), jsonMapper),
				SecurityContextHolderFilter.class);

		return http.build();
	}

	/**
	 * 默认权限映射器 (OIDC Scope 映射).
	 * @return GrantedAuthoritiesMapper
	 */
	@Bean
	@ConditionalOnMissingBean
	public GrantedAuthoritiesMapper oidcScopeGrantedAuthoritiesMapper() {
		return new OidcScopeGrantedAuthoritiesMapper();
	}

	/**
	 * 默认退出处理器.
	 * @param csrfTokenRepository CsrfTokenRepository
	 * @return LogoutHandler
	 */
	private LogoutHandler defaultLogoutHandler(CsrfTokenRepository csrfTokenRepository) {
		return new CompositeLogoutHandler(new SecurityContextLogoutHandler(),
				new CsrfLogoutHandler(csrfTokenRepository));
	}

	/**
	 * 审计日志退出处理器.
	 * <p>
	 * 仅当 LoginAuditHandler 存在时生效.
	 * @param loginAuditHandler 登录审计处理器
	 * @return LogoutHandler
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ConditionalOnBean(LoginAuditHandler.class)
	public LogoutHandler auditLogoutHandler(LoginAuditHandler loginAuditHandler) {
		return new AuditLogoutHandler(loginAuditHandler);
	}

	/**
	 * 密码编码器.
	 * <p>
	 * 使用 DelegatingPasswordEncoder 支持多种编码格式.
	 * @return PasswordEncoder
	 */
	@Bean
	@ConditionalOnMissingBean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/**
	 * OAuth2 Token 操作客户端.
	 * <p>
	 * 用于调用 Authorization Server 的 Token 端点 (如 /oauth2/revoke).
	 * @param restClientBuilder RestClient 构建器
	 * @param properties OAuth2 Client 配置属性
	 * @return OAuth2TokenClient
	 */
	@Bean
	@ConditionalOnMissingBean
	public OAuth2TokenClient oauth2TokenClient(RestClient.Builder restClientBuilder,
			OAuth2ClientProperties properties) {
		RestClient restClient = restClientBuilder.baseUrl(properties.getServerUrl())
			.defaultHeaders((headers) -> headers.setBasicAuth(properties.getClientId(), properties.getClientSecret()))
			.build();

		return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
			.build()
			.createClient(OAuth2TokenClient.class);
	}

	/**
	 * Token 吊销退出处理器.
	 * <p>
	 * 退出时调用 Authorization Server 的 /oauth2/revoke 端点吊销 Token.
	 * @param authorizedClientRepository OAuth2 授权客户端仓储
	 * @param tokenClient Token 操作客户端
	 * @param properties OAuth2 Client 配置属性
	 * @return LogoutHandler
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 100)
	public LogoutHandler revokeTokenLogoutHandler(OAuth2AuthorizedClientRepository authorizedClientRepository,
			OAuth2TokenClient tokenClient, OAuth2ClientProperties properties) {
		return new RevokeTokenLogoutHandler(authorizedClientRepository, tokenClient,
				properties.getDefaultRegistrationId());
	}

}
