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
 * MDC配置属性类, 用于配置MDC相关的属性.
 * <p>
 * 主要功能: 1. 配置traceId的生成和位置 2. 配置响应头中的traceId 3. 配置需要排除的路径
 * <p>
 * 配置示例: <pre>
 * logging:
 *   mdc:
 *     enabled: true
 *     target-converter: ch.qos.logback.classic.pattern.LevelConverter
 *     include-patterns:
 *       - /api/**
 *     exclude-patterns:
 *       - /api/health
 * </pre>
 *
 * @author Jax Jiang
 * @since 2025-06-09
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "logging.mdc")
public class MdcProperties {

	/**
	 * 是否启用MDC traceId功能.
	 * <p>
	 * 默认值: true. 配置项: logging.mdc.enabled.
	 */
	private boolean enabled = true;

	/**
	 * traceId在MDC (Mapped Diagnostic Context) 中的键名.
	 * <p>
	 * 这个键名将用于在日志格式中通过 {@code %X} 或 {@code %mdc} 来引用.
	 * <p>
	 * 默认值: {@code traceId}. 配置项: {@code logging.mdc.trace-id-key}.
	 */
	private String traceIdKey = "traceId";

	/**
	 * 定义将要注入到日志格式中的 traceId 内容模板.
	 * <p>
	 * 模板中的 {@code {traceIdKey}} 占位符会被实际的 {@code traceIdKey} 属性值动态替换.
	 * Logback 的默认值语法 (如 {@code :-}) 也是支持的, 用于在 traceId 不存在时提供默认值.
	 * <p>
	 * 默认值: {@code "[%X{{traceIdKey}:-}] "}. 配置项: {@code logging.mdc.trace-id-pattern}.
	 */
	private String traceIdPattern = "[%X{{traceIdKey}:-}] ";

	/**
	 * 指定traceId注入位置的目标转换器.
	 * <p>
	 * 用于定位注入点, 默认情况下, 会尝试将 traceId 注入到线程名 (ThreadConverter, 即 %thread 或 %t) 之后.
	 * 您可以配置为其他转换器的全限定类名, 例如: 'ch.qos.logback.classic.pattern.LevelConverter'.
	 * <p>
	 * 默认值: {@code ch.qos.logback.classic.pattern.ThreadConverter.class}. 配置项:
	 * logging.mdc.target-converter.
	 */
	private Class<? extends DynamicConverter<ILoggingEvent>> targetConverter = ThreadConverter.class;

	/**
	 * 是否在HTTP响应头中包含traceId.
	 * <p>
	 * 默认值: true. 配置项: logging.mdc.include-in-response.
	 */
	private boolean includeInResponse = true;

	/**
	 * 响应头中traceId的键名.
	 * <p>
	 * 默认值: X-Trace-Id. 配置项: logging.mdc.response-header-name.
	 */
	private String responseHeaderName = "X-Trace-Id";

	/**
	 * 需要包含 traceId 处理的 URL 路径模式.
	 * <p>
	 * 默认拦截所有路径. 配置项: logging.mdc.include-patterns.
	 */
	private String[] includePatterns = { "/**" };

	/**
	 * 需要排除 traceId 处理的 URL 路径模式.
	 * <p>
	 * 支持Ant风格的路径匹配. 配置项: logging.mdc.exclude-patterns.
	 */
	private String[] excludePatterns = { "/health", "/actuator/**", "/favicon.ico" };

	/**
	 * 获取最终经过处理的、将要注入到日志格式中的 traceId 内容.
	 * <p>
	 * 此方法会将 {@code traceIdPattern} 模板中的 {@code {traceIdKey}} 占位符
	 * 替换为当前配置的 {@code traceIdKey} 的值.
	 *
	 * @return 准备好注入的最终日志格式字符串.
	 */
	public String getTraceIdPattern() {
		return this.traceIdPattern.replace("{traceIdKey}", this.traceIdKey);
	}

}
