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

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 参数感知型 Accept-Header Locale 解析器测试.
 * <p>
 * 验证 {@link ParameterAwareAcceptHeaderLocaleResolver} 是否能正确地根据请求参数或 Accept-Header
 * 解析用户区域设置.
 *
 * @author Jax Jiang
 */
class ParameterAwareAcceptHeaderLocaleResolverTests {

	private final ParameterAwareAcceptHeaderLocaleResolver resolver = new ParameterAwareAcceptHeaderLocaleResolver();

	@Test
	void resolveLocaleShouldUseParameterIfPresent() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter(LocaleChangeInterceptor.DEFAULT_PARAM_NAME, "zh_CN");

		Locale locale = this.resolver.resolveLocale(request);

		assertThat(locale).isEqualTo(Locale.CHINA);
	}

	@Test
	void resolveLocaleShouldUseAcceptHeaderIfParameterMissing() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);

		Locale locale = this.resolver.resolveLocale(request);

		assertThat(locale).isEqualTo(Locale.US);
	}

	@Test
	void resolveLocaleShouldUseAcceptHeaderIfParameterEmpty() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.US);
		request.setParameter(LocaleChangeInterceptor.DEFAULT_PARAM_NAME, "");

		Locale locale = this.resolver.resolveLocale(request);

		assertThat(locale).isEqualTo(Locale.US);
	}

	@Test
	void setLocaleShouldDoNothing() {
		this.resolver.setLocale(new MockHttpServletRequest(), null, Locale.CHINA);
	}

}
