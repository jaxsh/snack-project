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

package org.jax.snack.framework.message.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TemplateRenderer 单元测试.
 *
 * @author Jax Jiang
 */
class TemplateRendererTests {

	private final TemplateRenderer renderer = new TemplateRenderer();

	@Test
	void shouldReplacePlaceholders() {
		String template = "Hello, ${name}!";
		Map<String, Object> params = Map.of("name", "World");
		String result = this.renderer.render(template, params);
		assertThat(result).isEqualTo("Hello, World!");
	}

	@Test
	void shouldUseDefaultValue() {
		String template = "Hello, ${name:Guest}!";
		Map<String, Object> params = Map.of();
		String result = this.renderer.render(template, params);
		assertThat(result).isEqualTo("Hello, Guest!");
	}

	@Test
	void shouldIgnoreUnresolvablePlaceholders() {
		String template = "Hello, ${name}!";
		Map<String, Object> params = Map.of();
		String result = this.renderer.render(template, params);
		assertThat(result).isEqualTo("Hello, ${name}!");
	}

	@Test
	void shouldSupportRecursion() {
		String template = "Hello, ${name:${defaultName:Unknown}}!";
		Map<String, Object> params = Map.of();
		String result = this.renderer.render(template, params);
		assertThat(result).isEqualTo("Hello, Unknown!");
	}

}
