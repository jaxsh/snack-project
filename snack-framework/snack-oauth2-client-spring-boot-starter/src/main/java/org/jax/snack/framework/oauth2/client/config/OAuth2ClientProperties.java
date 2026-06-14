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

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

/**
 * OAuth2 Client 配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snack.oauth2.client")
public class OAuth2ClientProperties {

	/**
	 * 应用名称.
	 * <p>
	 * 从 spring.application.name 自动注入, 用作 clientId 和 defaultRegistrationId 的默认值.
	 */
	private String applicationName;

	/**
	 * 是否启用 OAuth2 Client 自动配置.
	 */
	private boolean enabled = true;

	/**
	 * Authorization Server URL.
	 * <p>
	 * 用于 Token 吊销、后端通信等操作。默认配置由各模块 application.yml 中的环境变量 OAUTH_SERVER_URL 控制。
	 */
	private String serverUrl;

	/**
	 * Client ID.
	 * <p>
	 * 用于 Token 吊销时的身份验证. 默认使用 applicationName.
	 */
	private String clientId;

	/**
	 * 获取 Client ID.
	 * @return clientId, 如未配置则返回 applicationName
	 */
	public String getClientId() {
		return (this.clientId != null) ? this.clientId : this.applicationName;
	}

	/**
	 * Client Secret.
	 * <p>
	 * 用于 Token 吊销时的身份验证.
	 */
	private String clientSecret = "secret";

	/**
	 * 默认客户端注册 ID.
	 * <p>
	 * 默认使用 applicationName.
	 */
	private String defaultRegistrationId;

	/**
	 * 获取默认客户端注册 ID.
	 * @return defaultRegistrationId, 如未配置则返回 applicationName
	 */
	public String getDefaultRegistrationId() {
		return (this.defaultRegistrationId != null) ? this.defaultRegistrationId : this.applicationName;
	}

	/**
	 * 登录成功后默认跳转 URL.
	 */
	private String defaultSuccessUrl = "/";

	/**
	 * 允许匿名访问的路径.
	 */
	private List<String> permitAllPaths = List.of("/error", "/actuator/health", "/logout", "/login/oauth2/**");

	/**
	 * CSRF 校验豁免路径.
	 */
	private List<String> csrfIgnorePaths = List.of("/login", "/logout");

	/**
	 * REST API 路径前缀，未认证请求匹配此前缀时返回 401 JSON，其余路径重定向至登录.
	 */
	private String apiPathPattern = "/api/";

	/**
	 * OAuth2 授权端点基础路径.
	 */
	private String oauth2AuthorizationBaseUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI + "/";

	/**
	 * 获取 OAuth2 登录入口 URL（授权请求跳转地址）.
	 * @return 授权请求 URL，如 {@code /oauth2/authorization/snack-upms-server}
	 */
	public String getLoginUrl() {
		return this.oauth2AuthorizationBaseUri + getDefaultRegistrationId();
	}

	/**
	 * 外部登录页面地址.
	 */
	private String loginPage = "/login";

	/**
	 * SAS 的 OIDC 登出端点（end_session_endpoint）.
	 * <p>
	 * 配置示例：{@code ${snack.oauth2.client.server-url}/connect/logout}
	 */
	private String endSessionEndpointUri;

	/**
	 * 最大并发 Session 数.
	 * <p>
	 * -1 表示不限制；1 表示同时只允许一个设备登录，新登录踢出旧 Session.
	 */
	private int maxSessions = -1;

}
