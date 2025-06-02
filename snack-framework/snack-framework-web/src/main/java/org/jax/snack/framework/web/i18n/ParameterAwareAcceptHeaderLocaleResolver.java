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

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * 支持参数和Accept-Header的本地化解析器. 优先使用请求参数中的语言设置, 如果参数无效则回退到Accept-Header.
 *
 * @author Jax Jiang
 * @since 2025-05-31
 */
public class ParameterAwareAcceptHeaderLocaleResolver extends AcceptHeaderLocaleResolver {

	@NonNull
	@Override
	public Locale resolveLocale(HttpServletRequest request) {
		String localeParam = request.getParameter(LocaleChangeInterceptor.DEFAULT_PARAM_NAME);
		if (StringUtils.hasText(localeParam)) {
			Locale paramLocale = StringUtils.parseLocale(localeParam);
			if (paramLocale != null) {
				return paramLocale;
			}
		}
		return super.resolveLocale(request);
	}

	@Override
	public void setLocale(@NonNull HttpServletRequest request, @Nullable HttpServletResponse response,
			@Nullable Locale locale) {
	}

}
