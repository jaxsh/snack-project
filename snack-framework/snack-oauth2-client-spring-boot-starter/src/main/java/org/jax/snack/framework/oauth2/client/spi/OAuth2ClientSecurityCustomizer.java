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

package org.jax.snack.framework.oauth2.client.spi;

import java.util.Collections;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.web.authentication.logout.LogoutHandler;

/**
 * OAuth2 Client 安全配置定制器.
 * <p>
 * 各业务模块实现此接口以添加模块特有的安全逻辑.
 *
 * @author Jax Jiang
 */
public interface OAuth2ClientSecurityCustomizer extends Ordered {

	/**
	 * 定制 HttpSecurity.
	 * <p>
	 * 可以在此方法中添加模块特有的授权规则、过滤器等.
	 * @param http HttpSecurity builder
	 */
	void customize(HttpSecurity http);

	/**
	 * 配置授权规则.
	 * <p>
	 * 在框架的 anyRequest().authenticated() 之前调用, 用于配置更细粒度的路径权限.
	 * @param authorize 授权配置器
	 */
	default void configureAuthorization(
			AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authorize) {
	}

	/**
	 * 模块特有的 LogoutHandler 列表.
	 * <p>
	 * 这些处理器将在退出流程中被调用.
	 * @return LogoutHandler 列表, 可为空
	 */
	default List<LogoutHandler> logoutHandlers() {
		return Collections.emptyList();
	}

	/**
	 * 模块特有的权限映射器.
	 * <p>
	 * 用于将 OIDC/OAuth2 claims 转换为 Spring Security 权限.
	 * @return GrantedAuthoritiesMapper, 返回 null 表示使用默认
	 */
	default GrantedAuthoritiesMapper authoritiesMapper() {
		return null;
	}

	/**
	 * 登录成功后默认跳转 URL.
	 * @return URL 路径
	 */
	default String defaultSuccessUrl() {
		return "/";
	}

	/**
	 * 定制器优先级.
	 * <p>
	 * 数值越小优先级越高.
	 * @return 优先级
	 */
	@Override
	default int getOrder() {
		return 0;
	}

}
