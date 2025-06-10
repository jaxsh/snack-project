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
 * objectGUID到UUID的转换器. 用于将LDAP中的ObjectGUID转换为Java的UUID类型.
 *
 * @author Jax Jiang
 * @since 2025-05-30
 */
public class ObjectGUIDToUUIDConverter implements Converter<byte[], UUID> {

	/**
	 * 将字节数组形式的objectGUID转换为UUID.
	 * @param source objectGUID的字节数组
	 * @return 转换后的uuid对象
	 */
	@Override
	public UUID convert(@NonNull byte[] source) {
		return LdapUtils.byteArrayToGUID(source);
	}

}
