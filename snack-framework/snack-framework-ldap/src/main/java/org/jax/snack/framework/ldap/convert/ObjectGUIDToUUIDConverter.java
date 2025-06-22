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

import org.jax.snack.framework.ldap.util.LdapUtils;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

/**
 * 将 Active Directory 的 objectGUID (字节数组) 转换为 Java {@link UUID} 的转换器.
 *
 * @author Jax Jiang
 * @since 2025-05-30
 */
public class ObjectGUIDToUUIDConverter implements Converter<byte[], UUID> {

	/**
	 * 将 objectGUID 的字节数组表示形式转换为标准的 {@link UUID}.
	 * @param source ldap 中的 objectGUID 字节数组.
	 * @return 转换后的 {@link UUID} 对象.
	 */
	@Override
	public UUID convert(@NonNull byte[] source) {
		return LdapUtils.byteArrayToGUID(source);
	}

}
