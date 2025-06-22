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

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

/**
 * 将 Java {@link ZonedDateTime} 转换为 Active Directory FileTime 格式 (字符串) 的转换器.
 *
 * @author Jax Jiang
 * @since 2024-05-30
 */
public class ZonedDateTimeToFileTimeConverter implements Converter<ZonedDateTime, String> {

	/**
	 * 将 {@link ZonedDateTime} 对象转换为代表 100 纳秒间隔的 FileTime 字符串.
	 * @param source 要转换的 {@link ZonedDateTime} 对象.
	 * @return 转换后的 FileTime 格式字符串.
	 */
	@Override
	public String convert(@NonNull ZonedDateTime source) {
		return String.valueOf(LdapUtils.toLdapTimestamp(source));
	}

}
