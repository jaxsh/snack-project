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

package org.jax.snack.oauth.biz.security;

import org.jax.snack.oauth.biz.entity.OAuthUser;

import org.springframework.core.Ordered;

/**
 * 预授权限制策略.
 *
 * @author Jax Jiang
 */
public interface PreAuthRestriction extends Ordered {

	/**
	 * 分配给受限用户的 Spring Security 权限，如 {@code "SCOPE_pre_auth_reset"}.
	 * @return 权限字符串
	 */
	String getAuthority();

	/**
	 * 前端限制处理页的相对路径，如 {@code "/account/change-password"}.
	 * @return 相对路径
	 */
	String getPagePath();

	/**
	 * 判断该限制是否适用于给定用户.
	 * @param user 用户实体
	 * @return 是否适用
	 */
	boolean appliesTo(OAuthUser user);

	/**
	 * 限制首次触发时服务端回调（如 SMS/邮件发送验证码）. TOTP 默认空操作.
	 * @param username 用户名
	 */
	default void onApplied(String username) {
	}

}
