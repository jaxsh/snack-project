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
		String parentTraceId = "cf-trace-12345";
		MDC.put(TRACE_ID, parentTraceId);

		AtomicReference<String> traceIdInFuture = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			traceIdInFuture.set(MDC.get(TRACE_ID));
			latch.countDown();
		}, this.executor);

		future.get(2, TimeUnit.SECONDS);
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(traceIdInFuture.get()).isEqualTo(parentTraceId);

		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Test
	void shouldPropagateTraceIdThroughCompletableFutureChain() throws Exception {
		String parentTraceId = "cf-chain-trace";
		MDC.put(TRACE_ID, parentTraceId);

		AtomicReference<String> traceIdInFirstStage = new AtomicReference<>();
		AtomicReference<String> traceIdInSecondStage = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(2);

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

		assertThat(traceIdInFirstStage.get()).isEqualTo(parentTraceId);
		assertThat(traceIdInSecondStage.get()).isEqualTo(parentTraceId);
	}

	@Test
	void shouldClearMdcAfterCompletableFutureCompletes() throws Exception {
		String parentTraceId = "cf-cleanup-trace";
		MDC.put(TRACE_ID, parentTraceId);

		CountDownLatch latch = new CountDownLatch(1);

		CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
			MDC.put(TRACE_ID, "async-modified-trace");
			latch.countDown();
		}, this.executor);

		future.get(2, TimeUnit.SECONDS);
		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		Thread.sleep(100);

		assertThat(MDC.get(TRACE_ID)).isEqualTo(parentTraceId);
	}

	@Configuration
	static class CompletableFutureConfig {

	}

}
