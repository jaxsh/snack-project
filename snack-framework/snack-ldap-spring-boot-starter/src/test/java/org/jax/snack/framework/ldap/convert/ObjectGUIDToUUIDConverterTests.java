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

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 {@link ObjectGUIDToUUIDConverter} 的类型转换功能.
 * <p>
 * 验证将 Active Directory 的 objectGUID (混合字节序的字节数组) 正确转换为标准的 Java {@link java.util.UUID}.
 *
 * @author Jax Jiang
 */
class ObjectGUIDToUUIDConverterTests {

	private final ObjectGUIDToUUIDConverter converter = new ObjectGUIDToUUIDConverter();

	@Test
	void shouldConvertByteArrayToUUID() {
		byte[] objectGuidBytes = new byte[] { (byte) 0x77, (byte) 0x62, (byte) 0x22, (byte) 0x6b, (byte) 0x57,
				(byte) 0x33, (byte) 0x2e, (byte) 0x41, (byte) 0x97, (byte) 0xd6, (byte) 0xa5, (byte) 0x8f, (byte) 0x92,
				(byte) 0x37, (byte) 0x18, (byte) 0x61 };
		UUID expectedUuid = UUID.fromString("6b226277-3357-412e-97d6-a58f92371861");

		UUID result = this.converter.convert(objectGuidBytes);

		assertThat(result).isEqualTo(expectedUuid);
	}

}
