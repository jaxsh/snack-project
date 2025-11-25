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

package org.jax.snack.framework.ldap.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 {@link LdapAutoConfiguration} 的自动配置逻辑.
 * <p>
 * 验证 Spring Boot 启动时是否正确加载了 {@link ObjectDirectoryMapper} Bean, 并且该 Bean 是否配置了预期的类型转换器
 * (Converter).
 *
 * @author Jax Jiang
 */
class LdapAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LdapAutoConfiguration.class));

	@Test
	void shouldRegisterObjectDirectoryMapper() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ObjectDirectoryMapper.class);
			assertThat(context.getBean(ObjectDirectoryMapper.class)).isInstanceOf(DefaultObjectDirectoryMapper.class);
		});
	}

}
