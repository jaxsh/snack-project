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

package org.jax.snack.framework.mybatisplus.context;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 基于 Spring Security 的用户上下文提供者.
 * <p>
 * 从 SecurityContextHolder 获取当前认证用户的用户名.
 * <p>
 * 对于非 Web 场景 (如定时任务、异步任务等), 如果 SecurityContext 不存在或未认证, 则返回默认值 "system".
 *
 * @author Jax Jiang
 */
public class SecurityContextUserProvider implements UserContextProvider {

	private static final String DEFAULT_USER = "system";

	/**
	 * 从 Spring Security 上下文获取当前用户名.
	 * <p>
	 * Web 请求返回已认证用户名, 非 Web 场景 (定时任务/异步) 返回 "system".
	 * @return 当前用户名, 默认为 "system"
	 */
	@Override
	public String getCurrentUsername() {
		return Optional.of(SecurityContextHolder.getContext())
			.map(SecurityContext::getAuthentication)
			.map(Authentication::getName)
			.orElse(DEFAULT_USER);
	}

}
