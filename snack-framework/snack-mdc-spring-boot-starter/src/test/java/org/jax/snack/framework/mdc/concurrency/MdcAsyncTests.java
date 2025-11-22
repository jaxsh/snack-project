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

package org.jax.snack.framework.mdc.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jax.snack.framework.mdc.MdcAutoConfiguration;
import org.jax.snack.framework.mdc.MdcTaskDecorator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 @Async 异步方法中 traceId 的传播功能.
 * <p>
 * 验证在异步任务执行时，父线程的 traceId 能够正确传播到异步线程中.
 *
 * @author Jax Jiang
 * @since 2025-11-22
 */
@SpringBootTest(
		classes = { MdcAsyncTests.AsyncConfig.class, TaskExecutionAutoConfiguration.class, MdcAutoConfiguration.class },
		properties = { "logging.mdc.enabled=true" })
class MdcAsyncTests {

	private static final String TRACE_ID = "traceId";

	@Autowired
	private AsyncService asyncService;

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void shouldPropagateTraceIdToAsyncMethod() throws InterruptedException {
		// 在主线程中设置 traceId
		String parentTraceId = "parent-trace-12345";
		MDC.put(TRACE_ID, parentTraceId);

		AtomicReference<String> traceIdInAsync = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		// 调用异步方法
		this.asyncService.executeAsync(() -> {
			traceIdInAsync.set(MDC.get(TRACE_ID));
			latch.countDown();
		});

		// 等待异步任务完成
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		// 验证异步线程中能够获取到父线程的 traceId
		assertThat(traceIdInAsync.get()).isEqualTo(parentTraceId);

		// 验证主线程的 traceId 仍然存在
		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Test
	void shouldClearMdcAfterAsyncMethodCompletes() throws InterruptedException {
		String parentTraceId = "parent-trace-67890";
		MDC.put(TRACE_ID, parentTraceId);

		CountDownLatch latch = new CountDownLatch(1);

		this.asyncService.executeAsync(() -> {
			// 在异步方法中设置一个临时的 traceId
			MDC.put(TRACE_ID, "async-trace");
			latch.countDown();
		});

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		// 等待一段时间，确保异步线程已经清理了 MDC
		Thread.sleep(100);

		// 验证主线程的 traceId 不受影响
		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Configuration
	@EnableAsync
	static class AsyncConfig implements org.springframework.scheduling.annotation.AsyncConfigurer {

		@Autowired
		private MdcTaskDecorator mdcTaskDecorator;

		@Override
		public org.springframework.core.task.TaskExecutor getAsyncExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(2);
			executor.setMaxPoolSize(4);
			executor.setQueueCapacity(100);
			executor.setTaskDecorator(this.mdcTaskDecorator);
			executor.setThreadNamePrefix("async-test-");
			executor.initialize();
			return executor;
		}

		@Bean
		AsyncService asyncService() {
			return new AsyncService();
		}

	}

	@Service
	static class AsyncService {

		@Async
		void executeAsync(Runnable task) {
			task.run();
		}

	}

}
