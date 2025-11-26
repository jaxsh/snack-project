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

import ch.qos.logback.classic.pattern.ThreadConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;
import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MDC (Mapped Diagnostic Context) 功能配置属性类.
 * <p>
 * 定义了 Trace ID 的生成规则、日志注入行为及 Web 请求拦截规则等配置项. 对应配置文件前缀: {@code logging.mdc}.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "logging.mdc")
public class MdcProperties {

	/**
	 * 是否启用 MDC Trace ID 功能.
	 * <p>
	 * 默认为 {@code true}.
	 */
	private boolean enabled = true;

	/**
	 * MDC 中 Trace ID 的键名 (Key).
	 * <p>
	 * 该键名用于在日志配置文件中通过 {@code %X{...}} 引用.
	 * <p>
	 * 默认值: {@code traceId}.
	 */
	private String traceIdKey = "traceId";

	/**
	 * Trace ID 注入日志的格式模板.
	 * <p>
	 * 模板中的 {@code {traceIdKey}} 将被替换为实际的 {@link #traceIdKey} 值. 支持 Logback 默认值语法 (例如
	 * {@code :-}), 用于在 Trace ID 不存在时输出空字符串.
	 * <p>
	 * 默认值: {@code "[%X{{traceIdKey}:-}] "}.
	 */
	private String traceIdPattern = "[%X{{traceIdKey}:-}] ";

	/**
	 * 指定 Trace ID 注入在哪个 Logback 转换器 (Converter) 之后.
	 * <p>
	 * 系统会尝试在 Logback Pattern 中找到此转换器, 并将 Trace ID 插入其后. 默认为 {@link ThreadConverter} (即
	 * {@code %t} 或 {@code %thread}).
	 */
	private Class<? extends DynamicConverter<ILoggingEvent>> targetConverter = ThreadConverter.class;

	/**
	 * 是否将 Trace ID 添加到 HTTP 响应头中.
	 * <p>
	 * 默认为 {@code true}.
	 */
	private boolean includeInResponse = true;

	/**
	 * HTTP 响应头中 Trace ID 的字段名称.
	 * <p>
	 * 默认为 {@code X-Trace-Id}.
	 */
	private String responseHeaderName = "X-Trace-Id";

	/**
	 * 需要拦截并处理 Trace ID 的 URL 路径模式列表.
	 * <p>
	 * 默认为 {@code /**} (拦截所有请求).
	 */
	private String[] includePatterns = { "/**" };

	/**
	 * 需要排除 Trace ID 处理的 URL 路径模式列表.
	 * <p>
	 * 默认为: {@code /health}, {@code /actuator/**}, {@code /favicon.ico}.
	 */
	private String[] excludePatterns = { "/health", "/actuator/**", "/favicon.ico" };

	/**
	 * 获取解析后的 Trace ID 日志格式字符串.
	 * <p>
	 * 将 {@link #traceIdPattern} 中的 {@code {traceIdKey}} 占位符替换为实际的键名.
	 * @return 可直接用于 Logback Pattern 的字符串
	 */
	public String getTraceIdPattern() {
		return this.traceIdPattern.replace("{traceIdKey}", this.traceIdKey);
	}

}
