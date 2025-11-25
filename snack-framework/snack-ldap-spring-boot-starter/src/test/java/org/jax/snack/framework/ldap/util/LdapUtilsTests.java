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

package org.jax.snack.framework.ldap.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 测试 {@link LdapUtils} 工具类的核心功能.
 * <p>
 * 涵盖以下测试场景:
 * <ul>
 * <li>Active Directory objectGUID (Little-Endian) 到 Java UUID (Big-Endian) 的转换.</li>
 * <li>AD FileTime (1601 epoch) 与 Java ZonedDateTime (1970 epoch) 之间的双向转换.</li>
 * <li>异常输入 (如空数组、无效长度数组) 的处理逻辑.</li>
 * </ul>
 *
 * @author Jax Jiang
 */
class LdapUtilsTests {

	@Test
	void shouldConvertByteArrayToGuidCorrectly() {
		byte[] objectGuidBytes = new byte[] { (byte) 0x77, (byte) 0x62, (byte) 0x22, (byte) 0x6b, (byte) 0x57,
				(byte) 0x33, (byte) 0x2e, (byte) 0x41, (byte) 0x97, (byte) 0xd6, (byte) 0xa5, (byte) 0x8f, (byte) 0x92,
				(byte) 0x37, (byte) 0x18, (byte) 0x61 };

		UUID expectedUuid = UUID.fromString("6b226277-3357-412e-97d6-a58f92371861");

		UUID result = LdapUtils.byteArrayToGUID(objectGuidBytes);

		assertThat(result).isEqualTo(expectedUuid);
	}

	@Test
	void shouldThrowExceptionWhenByteArrayIsInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> LdapUtils.byteArrayToGUID(null))
			.withMessage("Input must be a 16-byte array.");

		assertThatIllegalArgumentException().isThrownBy(() -> LdapUtils.byteArrayToGUID(new byte[15]))
			.withMessage("Input must be a 16-byte array.");
	}

	@Test
	void shouldConvertFileTimeToZonedDateTime() {
		long fileTime = 133537248000000000L;
		ZonedDateTime expected = ZonedDateTime.of(LocalDateTime.of(2024, 3, 1, 0, 0, 0), ZoneOffset.UTC);

		ZonedDateTime result = LdapUtils.fileTimeToDateTime(fileTime);

		assertThat(result).isEqualTo(expected);
	}

	@Test
	void shouldReturnNullForZeroFileTime() {
		assertThat(LdapUtils.fileTimeToDateTime(0L)).isNull();
	}

	@Test
	void shouldReturnNullForMaxFileTime() {
		assertThat(LdapUtils.fileTimeToDateTime(Long.MAX_VALUE)).isNull();
	}

	@Test
	void shouldConvertZonedDateTimeToLdapTimestamp() {
		ZonedDateTime input = ZonedDateTime.of(LocalDateTime.of(2024, 3, 1, 0, 0, 0), ZoneOffset.UTC);
		long expected = 133537248000000000L;

		long result = LdapUtils.toLdapTimestamp(input);

		assertThat(result).isEqualTo(expected);
	}

	@Test
	void shouldReturnZeroForNullZonedDateTime() {
		assertThat(LdapUtils.toLdapTimestamp(null)).isEqualTo(0L);
	}

}
