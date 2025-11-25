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

package org.jax.snack.framework.ldap.convert;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 {@link ZonedDateTimeToFileTimeConverter} 的类型转换功能.
 * <p>
 * 验证将 Java 的 {@link java.time.ZonedDateTime} 正确转换为 Active Directory 的 FileTime 格式字符串.
 *
 * @author Jax Jiang
 */
class ZonedDateTimeToFileTimeConverterTests {

	private final ZonedDateTimeToFileTimeConverter converter = new ZonedDateTimeToFileTimeConverter();

	@Test
	void shouldConvertZonedDateTimeToStringFileTime() {
		ZonedDateTime input = ZonedDateTime.of(LocalDateTime.of(2024, 3, 1, 0, 0, 0), ZoneOffset.UTC);
		String expected = "133537248000000000";

		String result = this.converter.convert(input);

		assertThat(result).isEqualTo(expected);
	}

}
