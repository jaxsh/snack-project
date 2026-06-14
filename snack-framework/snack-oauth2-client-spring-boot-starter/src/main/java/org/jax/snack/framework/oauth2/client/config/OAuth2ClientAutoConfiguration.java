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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jax.snack.framework.oauth2.client.security.AuditLogoutHandler;
import org.jax.snack.framework.oauth2.client.security.CacheSessionRefreshLock;
import org.jax.snack.framework.oauth2.client.security.JsonExpiredSessionStrategy;
import org.jax.snack.framework.oauth2.client.security.JsonLogoutSuccessHandler;
import org.jax.snack.framework.oauth2.client.security.OidcScopeGrantedAuthoritiesMapper;
import org.jax.snack.framework.oauth2.client.security.RevokeTokenLogoutHandler;
import org.jax.snack.framework.oauth2.client.security.SessionRefreshLock;
import org.jax.snack.framework.oauth2.client.security.SessionStateCheckFilter;
import org.jax.snack.framework.oauth2.client.security.UsernameBasedSessionRegistry;
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
import org.springframework.cache.CacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
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
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
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
	 * @param sessionRefreshLock Session 刷新锁
	 * @param sessionRegistry Session 注册表
	 * @return SecurityFilterChain
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain oauth2ClientSecurityFilterChain(HttpSecurity http, OAuth2ClientProperties properties,
			ObjectProvider<OAuth2ClientSecurityCustomizer> customizers, ObjectProvider<LogoutHandler> logoutHandlers,
			@Qualifier("oidcScopeGrantedAuthoritiesMapper") ObjectProvider<GrantedAuthoritiesMapper> defaultMapperProvider,
			OAuth2AuthorizedClientManager authorizedClientManager, JsonMapper jsonMapper,
			SessionRefreshLock sessionRefreshLock, SessionRegistry sessionRegistry) {

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
			for (OAuth2ClientSecurityCustomizer customizer : sortedCustomizers) {
				customizer.configureAuthorization(authorize);
			}
			authorize.anyRequest().authenticated();
		})
			.oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()))
			.csrf((csrf) -> csrf.csrfTokenRepository(cookieCsrfTokenRepository)
				.csrfTokenRequestHandler(csrfTokenRequestAttributeHandler)
				.ignoringRequestMatchers(properties.getCsrfIgnorePaths().toArray(String[]::new)))
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

		String loginUrl = properties.getLoginUrl();
		http.logout((logout) -> {
			for (LogoutHandler handler : allLogoutHandlers) {
				logout.addLogoutHandler(handler);
			}
			logout.logoutSuccessHandler(
					new JsonLogoutSuccessHandler(loginUrl, properties.getEndSessionEndpointUri(), jsonMapper));
		});

		http.sessionManagement((session) -> session
			.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
			.sessionConcurrency((concurrency) -> {
				concurrency.maximumSessions(properties.getMaxSessions()).sessionRegistry(sessionRegistry);
				if (properties.getMaxSessions() > 0) {
					concurrency.expiredSessionStrategy(
							new JsonExpiredSessionStrategy(loginUrl, jsonMapper, oauth2ClientMessageSource()));
				}
			}));

		http.addFilterAfter(new SessionStateCheckFilter(authorizedClientManager, properties.getDefaultRegistrationId(),
				loginUrl, jsonMapper, sessionRefreshLock, oauth2ClientMessageSource()), SecurityContextHolderFilter.class);

		return http.build();
	}

	/**
	 * OAuth2 Client 专属消息源.
	 * <p>
	 * 独立于应用消息源，应用无需修改任何配置.
	 * @return MessageSource
	 */
	@Bean("oauth2ClientMessageSource")
	public MessageSource oauth2ClientMessageSource() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasename("classpath:i18n/oauth2-client");
		ms.setDefaultEncoding(StandardCharsets.UTF_8.name());
		return ms;
	}

	/**
	 * Session 刷新锁.
	 * <p>
	 * 基于 Spring Cache，后端由 {@code spring.cache.type} 决定：Caffeine 为本地锁，Redis 为分布式锁.
	 * @param cacheManagerProvider Spring CacheManager 提供者
	 * @return SessionRefreshLock
	 */
	@Bean
	@ConditionalOnMissingBean(SessionRefreshLock.class)
	public SessionRefreshLock sessionRefreshLock(ObjectProvider<CacheManager> cacheManagerProvider) {
		return new CacheSessionRefreshLock(cacheManagerProvider.getIfAvailable());
	}

	/**
	 * 发布 Session 销毁事件，{@link SessionRegistryImpl} 依赖此 Bean 感知 Session 过期.
	 * @return HttpSessionEventPublisher
	 */
	@Bean
	@ConditionalOnMissingBean(HttpSessionEventPublisher.class)
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
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

	/**
	 * Session 注册表（Spring Session 分布式模式）.
	 * <p>
	 * 仅当 spring-session 存在时生效，切换到分布式实现，代码无需改动.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.springframework.session.FindByIndexNameSessionRepository")
	static class SpringSessionRegistryConfiguration {

		/**
		 * Session 注册表（Redis 模式）.
		 * @param sessionRepository Spring Session Repository
		 * @return SessionRegistry
		 */
		@Bean
		@ConditionalOnMissingBean(SessionRegistry.class)
		SessionRegistry sessionRegistry(FindByIndexNameSessionRepository<?> sessionRepository) {
			return new SpringSessionBackedSessionRegistry<>(sessionRepository);
		}

	}

	/**
	 * Session 注册表（本地模式）.
	 * <p>
	 * spring-session 不在 classpath 时使用内存实现.
	 */
	@Configuration(proxyBeanMethods = false)
	static class LocalSessionRegistryConfiguration {

		/**
		 * Session 注册表（本地内存实现）.
		 * @return SessionRegistry
		 */
		@Bean
		@ConditionalOnMissingBean(SessionRegistry.class)
		SessionRegistry sessionRegistry() {
			return new UsernameBasedSessionRegistry();
		}

	}

}
