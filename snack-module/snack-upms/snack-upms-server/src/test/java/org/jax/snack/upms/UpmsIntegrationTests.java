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

import java.util.Map;

import org.jax.snack.framework.webtest.MockMvcTestSupport;
import org.jax.snack.upms.biz.security.UpmsSecurityMetadataManager;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * UPMS 模块集成测试基类.
 * <p>
 * 提供统一的配置导入和 Profile 激活，避免每个测试类重复定义.
 *
 * @author Jax Jiang
 */
@SpringBootTest(properties = { "spring.profiles.active=upms,oauth" })
@AutoConfigureMockMvc
@Transactional
public abstract class UpmsIntegrationTests extends MockMvcTestSupport {

	@MockitoBean
	private UpmsSecurityMetadataManager metadataManager;

	@BeforeEach
	void setUpPermissionRules() {
		Mockito.when(this.metadataManager.getPermissionRules())
			.thenReturn(Map.of(AnyRequestMatcher.INSTANCE, "test:permission"));
	}

}
