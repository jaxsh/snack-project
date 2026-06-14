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
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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
	 * 外部改密页面地址.
	 */
	private String changePasswordPage = "/account/change-password";

	/**
	 * 改密后触发 OAuth2 授权流程的地址.
	 * <p>
	 * 由 {@code OAuthFormLoginCustomizer} 在启动时根据 registrationId 自动填充；
	 * 改密成功且无 savedRequest 时，前端跳转至此以完成 token 颁发并获取完整权限.
	 */
	private String authorizationUri = "";

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
