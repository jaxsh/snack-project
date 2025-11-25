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
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MDC HTTP 请求拦截器.
 * <p>
 * 实现 {@link HandlerInterceptor} 接口，用于在 HTTP 请求处理全生命周期中管理 MDC 上下文. 主要功能：
 * <ul>
 * <li><b>Pre-Handle：</b> 生成或提取 Trace ID，放入 MDC，并可选写入响应头.</li>
 * <li><b>After-Completion：</b> 清理 MDC，防止线程池污染.</li>
 * </ul>
 *
 * @author Jax Jiang
 */
@Slf4j
@RequiredArgsConstructor
public class MdcInterceptor implements HandlerInterceptor {

	private final MdcProperties properties;

	private final TraceIdGenerator traceIdGenerator;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/**
	 * 请求前置处理.
	 * <p>
	 * 负责初始化 MDC 上下文.
	 * @param request HTTP 请求对象
	 * @param response HTTP 响应对象
	 * @param handler 被调用的处理器
	 * @return 总是返回 true
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
	 * 请求完成后的清理处理.
	 * <p>
	 * 无论请求成功还是异常，都必须执行 MDC 清理操作.
	 * @param request 当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param handler 执行的处理器
	 * @param exception 抛出的异常（如果有）
	 */
	@Override
	public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull Object handler, Exception exception) {
		if (this.isExcluded(request.getRequestURI())) {
			return;
		}
		MDC.clear();
	}

	/**
	 * 获取现有 Trace ID 或生成新 ID.
	 * <p>
	 * 优先从请求头中获取（用于分布式追踪衔接），如果不存在则使用生成器创建.
	 * @param request HTTP 请求
	 * @return 有效的 Trace ID 字符串
	 */
	private String getOrGenerateTraceId(HttpServletRequest request) {
		String traceId = request.getHeader(this.properties.getResponseHeaderName());
		if (StringUtils.hasText(traceId)) {
			return traceId;
		}
		return this.traceIdGenerator.generate();
	}

	/**
	 * 判断请求 URI 是否在排除名单中.
	 * @param requestURI 请求路径
	 * @return 如果匹配排除模式则返回 {@code true}
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
