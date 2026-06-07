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
import org.jspecify.annotations.Nullable;

/**
 * 活跃 Session 管理服务.
 *
 * @author Jax Jiang
 */
public interface SysSessionService {

	/**
	 * 查询活跃 Session 列表.
	 * @param username 用户名，为 null 时返回所有用户的 Session
	 * @return Session 列表
	 */
	List<SysSessionVO> getSessions(@Nullable String username);

	/**
	 * 踢出指定 Session.
	 * @param sessionId Session ID
	 */
	void revokeSession(String sessionId);

}
