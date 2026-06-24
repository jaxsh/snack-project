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

package org.jax.snack.upms;

import java.util.List;

import org.jax.snack.framework.webtest.MockMvcTestSupport;
import org.jax.snack.upms.biz.security.UpmsDynamicAuthorizationManager;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * UPMS 模块集成测试基类.
 * <p>
 * 提供统一的配置导入和 Profile 激活，避免每个测试类重复定义.
 *
 * @author Jax Jiang
 */
@SpringBootTest(properties = { "spring.profiles.active=upms,oauth,dev",
		"snack.oauth2.client.server-url=http://localhost:9000" })
@AutoConfigureMockMvc
@Transactional
@Import(UpmsIntegrationTests.CacheTestConfiguration.class)
public abstract class UpmsIntegrationTests extends MockMvcTestSupport {

	@MockitoBean
	protected UpmsDynamicAuthorizationManager dynamicAuthorizationManager;

	@TestConfiguration
	static class CacheTestConfiguration {

		@Bean
		CacheManager cacheManager() {
			SimpleCacheManager cacheManager = new SimpleCacheManager();
			cacheManager
				.setCaches(List.of(new ConcurrentMapCache("upms:user"), new ConcurrentMapCache("registered_clients")));
			return cacheManager;
		}

	}

}
