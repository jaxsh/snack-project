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

import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录审计处理器接口.
 * <p>
 * 各业务模块实现此接口以记录登录审计日志.
 *
 * @author Jax Jiang
 */
public interface LoginAuditHandler {

	/**
	 * 记录登录成功.
	 * @param username 用户名
	 * @param request HTTP 请求
	 */
	void recordLoginSuccess(String username, HttpServletRequest request);

	/**
	 * 记录登录失败.
	 * @param username 用户名
	 * @param reason 失败原因
	 * @param request HTTP 请求
	 */
	void recordLoginFailure(String username, String reason, HttpServletRequest request);

	/**
	 * 记录登出.
	 * @param username 用户名
	 * @param request HTTP 请求
	 */
	void recordLogout(String username, HttpServletRequest request);

}
