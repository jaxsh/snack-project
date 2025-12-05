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

package org.jax.snack.framework.web.i18n;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 时区拦截器.
 * <p>
 * 从请求头提取用户时区并设置到 LocaleContextHolder, 请求结束后自动清理.
 * <p>
 * 请求头: X-Timezone (可选), 例如: "Asia/Shanghai", "America/New_York".
 *
 * @author Jax Jiang
 */
@Slf4j
public class TimeZoneInterceptor implements HandlerInterceptor {

	private static final String TIMEZONE_HEADER = "X-Timezone";

	@Override
	public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Object handler) {
		String timezone = request.getHeader(TIMEZONE_HEADER);

		if (StringUtils.hasText(timezone)) {
			try {
				LocaleContextHolder.setTimeZone(TimeZone.getTimeZone(ZoneId.of(timezone)));
			}
			catch (DateTimeException ex) {
				log.debug("Invalid timezone from header: {}", timezone);
			}
		}

		return true;
	}

	@Override
	public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Object handler, Exception ex) {
		LocaleContextHolder.resetLocaleContext();
	}

}
