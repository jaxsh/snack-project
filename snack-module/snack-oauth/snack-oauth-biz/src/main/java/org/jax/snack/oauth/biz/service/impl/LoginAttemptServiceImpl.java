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

package org.jax.snack.oauth.biz.service.impl;

import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import org.jax.snack.oauth.biz.service.LoginAttemptService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 登录尝试服务实现.
 * <p>
 * 使用 Spring Cache 管理失败计数和锁定状态.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

	private static final String FAIL_COUNT_CACHE = "loginFailCount";

	private static final String LOCK_STATUS_CACHE = "loginLockStatus";

	private final ObjectProvider<CacheManager> cacheManagerProvider;

	@Override
	public synchronized int incrementFailCount(String username) {
		Cache cache = getCache(FAIL_COUNT_CACHE);
		if (cache == null) {
			return 1;
		}
		Integer count = cache.get(username, Integer.class);
		int newCount = (count != null) ? count + 1 : 1;
		cache.put(username, newCount);
		return newCount;
	}

	@Override
	public void resetFailCount(String username) {
		Cache cache = getCache(FAIL_COUNT_CACHE);
		if (cache != null) {
			cache.evict(username);
		}
	}

	@Override
	public int getFailCount(String username) {
		Cache cache = getCache(FAIL_COUNT_CACHE);
		if (cache == null) {
			return 0;
		}
		Integer count = cache.get(username, Integer.class);
		return (count != null) ? count : 0;
	}

	@Override
	public void setLockStatus(String username, ZonedDateTime lockUntil) {
		Cache cache = getCache(LOCK_STATUS_CACHE);
		if (cache != null) {
			cache.put(username, lockUntil);
		}
	}

	@Override
	public ZonedDateTime getLockUntil(String username) {
		Cache cache = getCache(LOCK_STATUS_CACHE);
		if (cache == null) {
			return null;
		}
		return cache.get(username, ZonedDateTime.class);
	}

	@Override
	public void clearLockStatus(String username) {
		Cache cache = getCache(LOCK_STATUS_CACHE);
		if (cache != null) {
			cache.evict(username);
		}
	}

	@Override
	public boolean isLocked(String username) {
		ZonedDateTime lockUntil = getLockUntil(username);
		if (lockUntil == null) {
			return false;
		}
		return ZonedDateTime.now().isBefore(lockUntil);
	}

	private Cache getCache(String cacheName) {
		CacheManager cacheManager = this.cacheManagerProvider.getIfAvailable();
		return (cacheManager != null) ? cacheManager.getCache(cacheName) : null;
	}

}
