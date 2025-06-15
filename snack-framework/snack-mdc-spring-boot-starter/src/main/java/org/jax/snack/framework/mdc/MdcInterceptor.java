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

package org.jax.snack.framework.mdc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.slf4j.MDC;

import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 一个用于管理 HTTP 请求的 MDC 上下文的 {@link HandlerInterceptor}.
 * <p>
 * 它在请求开始时设置一个唯一的 traceId, 并在请求完成后清理它. 此拦截器支持排除特定的 URL 模式, 并可以将 traceId 包含在 HTTP 响应头中.
 *
 * @author Jax Jiang
 * @since 2025-06-09
 */
@Slf4j
@RequiredArgsConstructor
public class MdcInterceptor implements HandlerInterceptor {

	private final MdcProperties properties;

	private final TraceIdGenerator traceIdGenerator;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 在请求被处理前设置 MDC 上下文. 它会获取或生成一个 traceId, 并将其放入 MDC 中.
	 * @param request 当前的 HTTP 请求.
	 * @param response 当前的 HTTP 响应.
	 * @param handler 被选择的处理器.
	 * @return 返回 {@code true} 以继续处理该请求.
	 */
	@Override
	public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Object handler) {
		if (this.isExcluded(request.getRequestURI())) {
			return true;
		}

		String traceId = this.getOrGenerateTraceId(request);
		MDC.put(this.properties.getTraceIdKey(), traceId);

		if (this.properties.isIncludeInResponse()) {
			response.setHeader(this.properties.getResponseHeaderName(), traceId);
		}

		return true;
	}

	/**
	 * 在请求完成后清理 MDC 上下文. 这对于防止线程间的上下文泄漏至关重要.
	 * @param request 当前的 HTTP 请求.
	 * @param response 当前的 HTTP 响应.
	 * @param handler 被执行的处理器.
	 * @param exception 处理器执行期间抛出的任何异常.
	 */
	@Override
	public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Object handler, Exception exception) {
		if (this.isExcluded(request.getRequestURI())) {
			return;
		}
		try {
			MDC.clear();
		}
		catch (Exception ex) {
			log.error("Failed to clear MDC context.", ex);
		}
	}

	/**
	 * 从请求头中获取一个已存在的 traceId, 或者使用注入的生成器生成一个新的.
	 * @param request 当前的 HTTP 请求.
	 * @return traceId 字符串.
	 */
	private String getOrGenerateTraceId(HttpServletRequest request) {
		String traceId = request.getHeader(this.properties.getResponseHeaderName());
		if (StringUtils.hasText(traceId)) {
			return traceId;
		}
		// 使用注入的生成器来创建新的 traceId
		return this.traceIdGenerator.generate();
	}

	/**
	 * 检查给定的请求 URI 是否应该被排除在 MDC 处理之外.
	 * @param requestURI 传入请求的 URI.
	 * @return 如果该 URI 匹配任何已配置的排除模式, 则返回 {@code true}.
	 */
	private boolean isExcluded(String requestURI) {
		for (String pattern : this.properties.getExcludePatterns()) {
			if (this.pathMatcher.match(pattern, requestURI)) {
				return true;
			}
		}
		return false;
	}

}
