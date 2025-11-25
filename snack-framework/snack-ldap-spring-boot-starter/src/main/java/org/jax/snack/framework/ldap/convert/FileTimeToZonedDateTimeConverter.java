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

import java.time.ZonedDateTime;

import org.jax.snack.framework.ldap.util.LdapUtils;
import org.jspecify.annotations.NonNull;

import org.springframework.core.convert.converter.Converter;

/**
 * 将 Active Directory 的 FileTime 格式 (字符串) 转换为 Java {@link ZonedDateTime} 的转换器.
 *
 * @author Jax Jiang
 */
public class FileTimeToZonedDateTimeConverter implements Converter<String, ZonedDateTime> {

	/**
	 * 将一个代表 100 纳秒间隔的 FileTime 字符串转换为 {@link ZonedDateTime}.
	 * @param source ldap 中 FileTime 格式的字符串.
	 * @return 转换后的 {@link ZonedDateTime} 对象, 时区为 UTC.
	 */
	@Override
	public ZonedDateTime convert(@NonNull String source) {
		return LdapUtils.fileTimeToDateTime(Long.parseLong(source));
	}

}
