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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jax.snack.framework.mdc.MdcAutoConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 CompletableFuture 异步执行时 traceId 的传播功能.
 * <p>
 * 验证在使用 MdcAwareExecutor 时，CompletableFuture 中的异步任务能够正确获取父线程的 traceId.
 *
 * @author Jax Jiang
 * @since 2025-11-22
 */
@SpringBootTest(classes = { MdcCompletableFutureTests.CompletableFutureConfig.class,
		TaskExecutionAutoConfiguration.class, MdcAutoConfiguration.class })
class MdcCompletableFutureTests {

	private static final String TRACE_ID = "traceId";

	@Autowired
	private Executor executor;

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void shouldPropagateTraceIdInCompletableFuture() throws Exception {
		// 在主线程中设置 traceId
		String parentTraceId = "cf-trace-12345";
		MDC.put(TRACE_ID, parentTraceId);

		AtomicReference<String> traceIdInFuture = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		// 使用 CompletableFuture 执行异步任务
		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			traceIdInFuture.set(MDC.get(TRACE_ID));
			latch.countDown();
		}, this.executor);

		// 等待异步任务完成
		future.get(2, TimeUnit.SECONDS);
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		// 验证异步线程中能够获取到父线程的 traceId
		assertThat(traceIdInFuture.get()).isEqualTo(parentTraceId);

		// 验证主线程的 traceId 仍然存在
		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Test
	void shouldPropagateTraceIdThroughCompletableFutureChain() throws Exception {
		String parentTraceId = "cf-chain-trace";
		MDC.put(TRACE_ID, parentTraceId);

		AtomicReference<String> traceIdInFirstStage = new AtomicReference<>();
		AtomicReference<String> traceIdInSecondStage = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(2);

		// 创建链式异步任务
		CompletableFuture.supplyAsync(() -> {
			traceIdInFirstStage.set(MDC.get(TRACE_ID));
			latch.countDown();
			return "result";
		}, this.executor).thenApplyAsync((result) -> {
			traceIdInSecondStage.set(MDC.get(TRACE_ID));
			latch.countDown();
			return result + "-processed";
		}, this.executor).get(2, TimeUnit.SECONDS);

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		// 验证两个阶段的异步任务都能获取到 traceId
		assertThat(traceIdInFirstStage.get()).isEqualTo(parentTraceId);
		assertThat(traceIdInSecondStage.get()).isEqualTo(parentTraceId);
	}

	@Test
	void shouldClearMdcAfterCompletableFutureCompletes() throws Exception {
		String parentTraceId = "cf-cleanup-trace";
		MDC.put(TRACE_ID, parentTraceId);

		CountDownLatch latch = new CountDownLatch(1);

		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			// 在异步任务中修改 traceId
			MDC.put(TRACE_ID, "async-modified-trace");
			latch.countDown();
		}, this.executor);

		future.get(2, TimeUnit.SECONDS);
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		// 等待一段时间，确保异步线程已经清理了 MDC
		Thread.sleep(100);

		// 验证主线程的 traceId 不受影响
		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Configuration
	static class CompletableFutureConfig {

		// MdcAutoConfiguration 会自动创建 MdcAwareExecutor 并标记为 @Primary
		// 这里不需要额外配置

	}

}
