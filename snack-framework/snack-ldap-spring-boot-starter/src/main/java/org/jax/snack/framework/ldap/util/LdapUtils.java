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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Ldap工具类.
 *
 * @author Jax Jiang
 */
public final class LdapUtils {

	private static final LocalDateTime FILETIME_EPOCH = LocalDateTime.of(1601, 1, 1, 0, 0, 0);

	private static final LocalDateTime UNIX_EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

	private static final long FILETIME_TO_UNIX_EPOCH_SECONDS = ChronoUnit.SECONDS
		.between(FILETIME_EPOCH.atZone(ZoneOffset.UTC), UNIX_EPOCH.atZone(ZoneOffset.UTC));

	private static final long HUNDRED_NANOS_PER_SECOND = 10_000_000L;

	private static final long FILETIME_TO_UNIX_EPOCH_HUNDRED_NANOS = FILETIME_TO_UNIX_EPOCH_SECONDS
			* HUNDRED_NANOS_PER_SECOND;

	private LdapUtils() {
	}

	/**
	 * 将 Active Directory 的 objectGUID (字节数组) 转换为标准的 {@link UUID}.
	 *
	 * <p>
	 * 此方法处理 AD objectGUID 与 Java UUID 之间的字节序差异.
	 * @param source 16 字节的 objectGUID 字节数组.
	 * @return 转换后的 {@link UUID} 对象.
	 * @throws IllegalArgumentException 如果输入不是 16 字节的数组.
	 */
	public static UUID byteArrayToGUID(byte[] source) {
		if (source == null || source.length != 16) {
			throw new IllegalArgumentException("Input must be a 16-byte array.");
		}

		long mostSigBits = 0;
		mostSigBits |= (long) (source[3] & 0xFF) << 56;
		mostSigBits |= (long) (source[2] & 0xFF) << 48;
		mostSigBits |= (long) (source[1] & 0xFF) << 40;
		mostSigBits |= (long) (source[0] & 0xFF) << 32;
		mostSigBits |= (long) (source[5] & 0xFF) << 24;
		mostSigBits |= (long) (source[4] & 0xFF) << 16;
		mostSigBits |= (long) (source[7] & 0xFF) << 8;
		mostSigBits |= source[6] & 0xFF;

		long leastSigBits = 0;
		for (int i = 8; i < 16; i++) {
			leastSigBits <<= 8;
			leastSigBits |= (source[i] & 0xFF);
		}

		return new UUID(mostSigBits, leastSigBits);
	}

	/**
	 * 将 AD FileTime (LDAP 时间戳) 转换为 {@link ZonedDateTime}.
	 * @param ldapTimestamp ldap 时间戳.
	 * @return 转换后的 {@link ZonedDateTime} 对象 (UTC 时区), 如果时间戳无效则返回 null.
	 */
	public static ZonedDateTime fileTimeToDateTime(long ldapTimestamp) {
		if (ldapTimestamp == 0 || ldapTimestamp == Long.MAX_VALUE) {
			return null;
		}

		long hundredNanosSinceUnixEpoch = ldapTimestamp - FILETIME_TO_UNIX_EPOCH_HUNDRED_NANOS;
		long seconds = hundredNanosSinceUnixEpoch / HUNDRED_NANOS_PER_SECOND;
		long nanoOfSecond = (hundredNanosSinceUnixEpoch % HUNDRED_NANOS_PER_SECOND) * 100;

		Instant instant = Instant.ofEpochSecond(seconds, nanoOfSecond);
		return ZonedDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	/**
	 * 将 {@link ZonedDateTime} 转换为 AD FileTime (LDAP 时间戳).
	 * @param zonedDateTime 要转换的 ZonedDateTime 对象.
	 * @return 对应的 LDAP 时间戳.
	 */
	public static long toLdapTimestamp(ZonedDateTime zonedDateTime) {
		if (zonedDateTime == null) {
			return 0L;
		}

		long secondsSinceUnixEpoch = zonedDateTime.toEpochSecond();
		// 将纳秒部分转换为100纳秒单位
		long hundredNanosOfSecond = zonedDateTime.getNano() / 100;

		long hundredNanosSinceUnixEpoch = (secondsSinceUnixEpoch * HUNDRED_NANOS_PER_SECOND) + hundredNanosOfSecond;

		return hundredNanosSinceUnixEpoch + FILETIME_TO_UNIX_EPOCH_HUNDRED_NANOS;
	}

}
