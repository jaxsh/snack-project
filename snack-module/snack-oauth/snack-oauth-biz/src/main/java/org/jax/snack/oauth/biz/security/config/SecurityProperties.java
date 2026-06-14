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

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.jax.snack.oauth.biz.security.PreAuthRestriction;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 安全策略配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "snack.security.policy")
public class SecurityProperties {

	/**
	 * 密码有效期 (天).
	 * <p>
	 * 默认: 90. 设置为 0 或 -1 表示禁用.
	 */
	private int passwordExpirationDays = 90;

	/**
	 * 最大登录尝试次数 (锁定阈值).
	 * <p>
	 * 默认: 5. 设置为 0 表示禁用.
	 */
	private int maxLoginAttempts = 5;

	/**
	 * 阶梯锁定时长 (分钟).
	 * <p>
	 * 按锁定次数递增使用, 0 表示永久锁定.
	 * <p>
	 * 默认: [5, 30, 0] (第1次5分钟, 第2次30分钟, 第3次永久).
	 */
	private List<Integer> lockDurations = List.of(5, 30, 0);

	/**
	 * 是否强制首次登录修改密码.
	 */
	private boolean forceChangeInitialPassword = true;

	/**
	 * 新用户默认密码.
	 */
	private String defaultPassword = "Snack@123";

	/**
	 * 外部登录页面地址.
	 */
	private String loginPage = "/login";

	/**
	 * 前端应用 base URL，用于 pre-auth restriction 重定向.
	 * <p>
	 * 未显式配置时自动从 {@link #loginPage} 提取（如 {@code http://localhost:8000/user/login} → {@code
	 * http://localhost:8000}）；{@code loginPage} 为相对路径时返回空字符串，过滤器以相对路径重定向.
	 */
	private String frontendBaseUrl = "";

	/**
	 * 获取前端应用 base URL.
	 * <p>
	 * 未显式配置时从 {@link #loginPage} 自动提取.
	 * @return base URL，如 {@code http://localhost:8000}，或空字符串
	 */
	public String getFrontendBaseUrl() {
		if (StringUtils.hasText(this.frontendBaseUrl)) {
			return this.frontendBaseUrl;
		}
		UriComponents uri = UriComponentsBuilder.fromUriString(this.loginPage).build();
		if (uri.getScheme() == null) {
			return "";
		}
		int port = uri.getPort();
		return uri.getScheme() + "://" + uri.getHost() + ((port > 0) ? ":" + port : "");
	}

	/**
	 * 无需认证即可访问的路径列表.
	 */
	private List<String> permitAllPaths = List.of("/error", "/actuator/health");

	/**
	 * CSRF 校验豁免路径.
	 */
	private List<String> csrfIgnorePaths = List.of("/login", "/oauth2/account/**");

	/**
	 * 退出登录触发路径.
	 */
	private String logoutUrl = "/logout";

	/**
	 * 各预授权限制对应的前端页面路径.
	 * <p>
	 * key 为 restriction 的 scope key（如 {@code "pre_auth_reset"}），value 为前端相对路径（如
	 * {@code "/account/change-password"}）. 各 {@link PreAuthRestriction} 实现通过 scope key 在此
	 * Map 中查询自身的处理页路径. 通过 {@code snack.security.policy.pre-auth-pages} 配置.
	 */
	private Map<String, String> preAuthPages = new LinkedHashMap<>();

	/**
	 * 判断凭证是否已过期.
	 * @param lastPasswordResetTime 最近一次密码重置时间
	 * @return 是否过期
	 */
	public boolean isCredentialsExpired(ZonedDateTime lastPasswordResetTime) {
		if (getPasswordExpirationDays() <= 0 || lastPasswordResetTime == null) {
			return false;
		}
		return ZonedDateTime.now().isAfter(lastPasswordResetTime.plusDays(getPasswordExpirationDays()));
	}

}
