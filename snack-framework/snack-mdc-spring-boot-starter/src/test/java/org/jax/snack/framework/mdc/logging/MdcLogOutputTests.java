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

package org.jax.snack.framework.mdc.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试最终的日志输出结果.
 * <p>
 * 验证 Logback 真正渲染出来的日志字符串中是否包含了 MDC 中的 traceId. 这是对 "MDC 上下文 -> Logback 配置 -> 最终日志输出"
 * 完整链路的端到端验证.
 *
 * @author Jax Jiang
 * @since 2025-11-22
 */
@SpringBootTest(classes = MdcLogOutputTests.LogOutputConfig.class, properties = { "logging.mdc.enabled=true",
		// 为了便于断言，我们可以自定义一个独特的 pattern，或者使用默认的
		// 默认是 [%X{traceId:-}]，所以 traceId 会被 [] 包裹
		"logging.mdc.trace-id-key=traceId" })
@ExtendWith(OutputCaptureExtension.class)
class MdcLogOutputTests {

	private static final Logger log = LoggerFactory.getLogger(MdcLogOutputTests.class);

	@Test
	void shouldOutputTraceIdInConsoleLog(CapturedOutput output) {
		// 1. 设置 MDC
		String traceId = "test-log-output-12345";
		MDC.put("traceId", traceId);

		try {
			// 2. 打印一条日志
			log.info("This is a test log message");

			// 3. 验证输出
			// 期望输出类似: ... [test-log-output-12345] ... This is a test log message ...
			assertThat(output.getOut()).contains(traceId);
			// 验证默认的格式 (被中括号包裹)
			assertThat(output.getOut()).contains("[" + traceId + "]");
			assertThat(output.getOut()).contains("This is a test log message");
		}
		finally {
			MDC.clear();
		}
	}

	@Test
	void shouldNotOutputTraceIdWhenMdcIsEmpty(CapturedOutput output) {
		// 1. 确保 MDC 为空
		MDC.clear();

		// 2. 打印日志
		log.info("Log without traceId");

		// 3. 验证输出
		// 根据默认配置 pattern: [%X{traceId:-}]
		// 当 traceId 为空时，Logback 的 :- 语法应该输出空字符串，或者仅仅保留外层的结构（取决于配置的 pattern）
		// 如果 pattern 是 "[%X{traceId:-}] "，那么空的时候可能是 "[] " 或者仅仅是一个空格，这取决于
		// LogbackConfigurer 如何注入
		// 让我们先观察输出，通常我们期望它要么是空的，要么是一个占位符

		// 检查日志内容存在
		assertThat(output.getOut()).contains("Log without traceId");

		// 确保之前的 traceId 不存在 (防止污染)
		assertThat(output.getOut()).doesNotContain("test-log-output-12345");
	}

	@Configuration
	@EnableAutoConfiguration
	static class LogOutputConfig {

		// 加载 MdcAutoConfiguration 及其依赖

	}

}
