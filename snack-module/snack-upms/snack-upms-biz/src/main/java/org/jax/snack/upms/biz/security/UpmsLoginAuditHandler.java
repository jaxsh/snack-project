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

package org.jax.snack.upms.biz.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.oauth2.client.spi.LoginAuditHandler;
import org.jax.snack.framework.web.utils.IpUtils;
import org.jax.snack.upms.biz.entity.SysLoginLog;
import org.jax.snack.upms.biz.repository.SysLoginLogRepository;

import org.springframework.stereotype.Component;

/**
 * UPMS 登录审计处理器实现.
 * <p>
 * 实现 LoginAuditHandler SPI, 将登录日志记录到 sys_login_log 表.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpmsLoginAuditHandler implements LoginAuditHandler {

	private static final String ACTION_LOGIN_SUCCESS = "LOGIN_SUCCESS";

	private static final String ACTION_LOGIN_FAILURE = "LOGIN_FAILURE";

	private static final String ACTION_LOGOUT = "LOGOUT";

	private static final int MAX_USER_AGENT_LENGTH = 500;

	private final SysLoginLogRepository loginLogRepository;

	@Override
	public void recordLoginSuccess(String username, HttpServletRequest request) {
		saveLoginLog(username, ACTION_LOGIN_SUCCESS, null, request);
		log.info("Login success for user [{}]", username);
	}

	@Override
	public void recordLoginFailure(String username, String reason, HttpServletRequest request) {
		saveLoginLog(username, ACTION_LOGIN_FAILURE, reason, request);
		log.warn("Login failure for user [{}], reason: {}", username, reason);
	}

	@Override
	public void recordLogout(String username, HttpServletRequest request) {
		saveLoginLog(username, ACTION_LOGOUT, null, request);
		log.info("Logout for user [{}]", username);
	}

	/**
	 * 保存登录日志.
	 * @param username 用户名
	 * @param action 事件类型
	 * @param failureReason 失败原因
	 * @param request HTTP 请求
	 */
	private void saveLoginLog(String username, String action, String failureReason, HttpServletRequest request) {
		SysLoginLog loginLog = new SysLoginLog();
		loginLog.setUsername(username);
		loginLog.setAction(action);
		loginLog.setFailureReason(failureReason);

		if (request != null) {
			loginLog.setIpAddress(IpUtils.getIpAddr(request));
			loginLog.setUserAgent(truncate(request.getHeader("User-Agent")));

			HttpSession session = request.getSession(false);
			if (session != null) {
				loginLog.setSessionId(session.getId());
			}
		}

		this.loginLogRepository.save(loginLog);
	}

	/**
	 * 截断字符串.
	 * @param str 原字符串
	 * @return 截断后的字符串
	 */
	private String truncate(String str) {
		if (str == null || str.length() <= MAX_USER_AGENT_LENGTH) {
			return str;
		}
		return str.substring(0, MAX_USER_AGENT_LENGTH);
	}

}
