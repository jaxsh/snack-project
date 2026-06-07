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

package org.jax.snack.framework.oauth2.client.security;

import java.util.concurrent.TimeUnit;

/**
 * Session 刷新锁.
 * <p>
 * 用于防止同一 Session 的并发请求同时触发 AT 刷新，避免 RT 轮换场景下的竞争问题. 单实例部署时由
 * {@link CacheSessionRefreshLock} 基于本地缓存实现； 多实例部署时切换 {@code spring.cache.type=redis}
 * 即可自动升级为分布式锁，无需改代码.
 *
 * @author Jax Jiang
 */
public interface SessionRefreshLock {

	/**
	 * 尝试获取锁.
	 * @param sessionId Session ID
	 * @param timeout 超时时间
	 * @param unit 时间单位
	 * @return 是否成功获取锁
	 * @throws InterruptedException 线程中断
	 */
	boolean tryLock(String sessionId, long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * 释放锁.
	 * @param sessionId Session ID
	 */
	void unlock(String sessionId);

}
