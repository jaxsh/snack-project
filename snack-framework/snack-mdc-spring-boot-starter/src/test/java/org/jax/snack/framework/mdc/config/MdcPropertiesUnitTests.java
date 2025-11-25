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

package org.jax.snack.framework.mdc.config;

import org.jax.snack.framework.mdc.MdcProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MdcProperties} 的单元测试.
 * <p>
 * 验证属性类的默认值以及占位符替换逻辑.
 *
 * @author Jax Jiang
 */
class MdcPropertiesUnitTests {

	@Test
	void shouldReplacePlaceholderWithTraceIdKey() {
		MdcProperties properties = new MdcProperties();
		properties.setTraceIdKey("customTraceId");
		properties.setTraceIdPattern("|TID:%X{{traceIdKey}:-}| ");

		assertThat(properties.getTraceIdPattern()).isEqualTo("|TID:%X{customTraceId:-}| ");
	}

	@Test
	void shouldExposeReasonableDefaults() {
		MdcProperties properties = new MdcProperties();

		assertThat(properties.isEnabled()).isTrue();
		assertThat(properties.getTraceIdKey()).isEqualTo("traceId");
		assertThat(properties.getTraceIdPattern()).isEqualTo("[%X{traceId:-}] ");
		assertThat(properties.getTargetConverter().getName())
			.isEqualTo("ch.qos.logback.classic.pattern.ThreadConverter");
		assertThat(properties.isIncludeInResponse()).isTrue();
		assertThat(properties.getResponseHeaderName()).isEqualTo("X-Trace-Id");
		assertThat(properties.getIncludePatterns()).containsExactly("/**");
		assertThat(properties.getExcludePatterns()).containsExactly("/health", "/actuator/**", "/favicon.ico");
	}

}
