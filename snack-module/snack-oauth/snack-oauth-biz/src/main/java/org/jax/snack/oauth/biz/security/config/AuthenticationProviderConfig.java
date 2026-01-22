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

import org.jax.snack.oauth.biz.security.CacheBasedUserDetailsChecker;
import org.jax.snack.oauth.biz.security.LockCheckingUserDetailsService;
import org.jax.snack.oauth.biz.service.LoginAttemptService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证提供者配置.
 *
 * @author Jax Jiang
 */
@Configuration
public class AuthenticationProviderConfig {

	/**
	 * 认证提供者.
	 * <p>
	 * 配置认证提供者, 使用装饰后的 UserDetailsService 在查询数据库前检查缓存锁定状态, 并关联预认证检查器以处理数据库锁定逻辑.
	 * @param userDetailsService 用户详情服务
	 * @param passwordEncoder 密码编码器
	 * @param preAuthChecker 预认证检查器
	 * @param loginAttemptService 登录尝试服务
	 * @return DaoAuthenticationProvider
	 */
	@Bean
	public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder, CacheBasedUserDetailsChecker preAuthChecker,
			LoginAttemptService loginAttemptService) {
		UserDetailsService lockChecking = new LockCheckingUserDetailsService(userDetailsService, loginAttemptService);
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(lockChecking);
		provider.setPasswordEncoder(passwordEncoder);
		provider.setPreAuthenticationChecks(preAuthChecker);
		return provider;
	}

}
