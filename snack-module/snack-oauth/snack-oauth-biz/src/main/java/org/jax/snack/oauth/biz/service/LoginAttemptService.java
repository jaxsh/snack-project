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

package org.jax.snack.oauth.biz.service;

import java.time.ZonedDateTime;

/**
 * 登录尝试服务.
 * <p>
 * 管理缓存中的登录失败计数和锁定状态.
 *
 * @author Jax Jiang
 */
public interface LoginAttemptService {

	/**
	 * 递增失败次数.
	 * @param username 用户名
	 * @return 当前失败次数
	 */
	int incrementFailCount(String username);

	/**
	 * 重置失败次数.
	 * @param username 用户名
	 */
	void resetFailCount(String username);

	/**
	 * 获取失败次数.
	 * @param username 用户名
	 * @return 失败次数
	 */
	int getFailCount(String username);

	/**
	 * 设置锁定状态到缓存.
	 * @param username 用户名
	 * @param lockUntil 锁定截止时间
	 */
	void setLockStatus(String username, ZonedDateTime lockUntil);

	/**
	 * 获取缓存中的锁定截止时间.
	 * @param username 用户名
	 * @return 锁定截止时间, null 表示未锁定
	 */
	ZonedDateTime getLockUntil(String username);

	/**
	 * 清除锁定状态.
	 * @param username 用户名
	 */
	void clearLockStatus(String username);

	/**
	 * 检查是否处于锁定状态.
	 * @param username 用户名
	 * @return 是否锁定
	 */
	boolean isLocked(String username);

}
