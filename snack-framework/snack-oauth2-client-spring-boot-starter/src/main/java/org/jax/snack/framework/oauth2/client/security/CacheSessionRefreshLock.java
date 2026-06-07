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

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/**
 * 基于 Spring Cache 的 Session 刷新锁.
 * <p>
 * 通过 {@link Cache#putIfAbsent} 实现互斥：Caffeine 后端使用 compute 原子操作， Redis 后端使用 SET NX，切换后端只需改
 * {@code spring.cache.type}，代码无需变动.
 *
 * @author Jax Jiang
 */
public class CacheSessionRefreshLock implements SessionRefreshLock {

	static final String CACHE_NAME = "session-refresh-locks";

	private static final String LOCK_VALUE = "1";

	@Nullable
	private final CacheManager cacheManager;

	public CacheSessionRefreshLock(@Nullable CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public boolean tryLock(String sessionId, long timeout, TimeUnit unit) throws InterruptedException {
		if (this.cacheManager == null) {
			return true;
		}
		Cache cache = this.cacheManager.getCache(CACHE_NAME);
		if (cache == null) {
			return true;
		}
		long deadline = System.nanoTime() + unit.toNanos(timeout);
		do {
			if (cache.putIfAbsent(sessionId, LOCK_VALUE) == null) {
				return true;
			}
			TimeUnit.MILLISECONDS.sleep(50);
		}
		while (System.nanoTime() < deadline);
		return false;
	}

	@Override
	public void unlock(String sessionId) {
		if (this.cacheManager == null) {
			return;
		}
		Cache cache = this.cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			cache.evict(sessionId);
		}
	}

}
