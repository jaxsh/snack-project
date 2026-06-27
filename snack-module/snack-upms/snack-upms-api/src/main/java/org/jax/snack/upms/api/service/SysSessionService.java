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

package org.jax.snack.upms.api.service;

import java.util.List;

import org.jax.snack.upms.api.vo.SysSessionVO;

/**
 * 活跃 Session 管理服务.
 *
 * @author Jax Jiang
 */
public interface SysSessionService {

	/**
	 * 查询指定用户的活跃 Session 列表.
	 * @param username 用户名
	 * @return Session 列表
	 */
	List<SysSessionVO> getSessions(String username);

	/**
	 * 踢出指定用户的活跃 Session.
	 * @param username 用户名
	 * @param sessionId Session ID (为 null 或空时清退该用户的所有会话)
	 */
	void revokeSession(String username, String sessionId);

}
