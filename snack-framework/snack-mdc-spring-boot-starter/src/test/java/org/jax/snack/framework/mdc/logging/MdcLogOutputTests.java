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
		String traceId = "test-log-output-12345";
		MDC.put("traceId", traceId);

		try {
			log.info("This is a test log message");

			assertThat(output.getOut()).contains(traceId);
			assertThat(output.getOut()).contains("[" + traceId + "]");
			assertThat(output.getOut()).contains("This is a test log message");
		}
		finally {
			MDC.clear();
		}
	}

	@Test
	void shouldNotOutputTraceIdWhenMdcIsEmpty(CapturedOutput output) {
		MDC.clear();

		log.info("Log without traceId");

		assertThat(output.getOut()).contains("Log without traceId");

		assertThat(output.getOut()).doesNotContain("test-log-output-12345");
	}

	@Configuration
	@EnableAutoConfiguration
	static class LogOutputConfig {

	}

}
