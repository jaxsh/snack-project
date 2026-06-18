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

package org.jax.snack.oauth.server;

import org.jax.snack.framework.webtest.MockMvcTestSupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 模块集成测试基类.
 * <p>
 * 提供统一的配置导入和 Profile 激活.
 *
 * @author Jax Jiang
 */
@SpringBootTest(properties = { "spring.profiles.active=oauth,dev" })
@AutoConfigureMockMvc
@Transactional
public abstract class OAuthIntegrationTests extends MockMvcTestSupport {

}
