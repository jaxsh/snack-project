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

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jax.snack.framework.mdc.MdcProperties;
import org.jax.snack.framework.mdc.MdcSchedulingConfigurer;
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 @Scheduled 定时任务中 traceId 的生成功能.
 * <p>
 * 验证定时任务执行时，能够自动生成新的 traceId 并在任务完成后清理.
 *
 * @author Jax Jiang
 */
class MdcScheduledTests {

	private static final String TRACE_ID = "traceId";

	private GenericApplicationContext applicationContext;

	private ThreadPoolTaskScheduler scheduler;

	@BeforeEach
	void setUp() {
		this.applicationContext = new GenericApplicationContext();
		this.applicationContext.registerBean(MdcProperties.class, MdcProperties::new);
		this.applicationContext.registerBean(TraceIdGenerator.class, FixedTraceIdGenerator::new);
		this.applicationContext.refresh();

		this.scheduler = new ThreadPoolTaskScheduler();
		this.scheduler.initialize();
	}

	@AfterEach
	void tearDown() {
		if (this.scheduler != null) {
			this.scheduler.shutdown();
		}
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
		MDC.clear();
	}

	@Test
	void shouldGenerateTraceIdForScheduledTask() throws InterruptedException {
		MdcSchedulingConfigurer configurer = new MdcSchedulingConfigurer();
		configurer.setApplicationContext(this.applicationContext);

		Object wrapped = Objects
			.requireNonNull(configurer.postProcessAfterInitialization(this.scheduler, "taskScheduler"));
		assertThat(wrapped).isInstanceOf(TaskScheduler.class);
		TaskScheduler taskScheduler = (TaskScheduler) wrapped;

		AtomicReference<String> traceIdInTask = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		taskScheduler.schedule(() -> {
			traceIdInTask.set(MDC.get(TRACE_ID));
			latch.countDown();
		}, new ImmediateTrigger());

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(traceIdInTask.get()).isNotNull();
		assertThat(traceIdInTask.get()).isEqualTo("fixed-trace-id");

		assertThat(MDC.get(TRACE_ID)).isNull();
	}

	@Test
	void shouldGenerateDifferentTraceIdForEachScheduledTask() throws InterruptedException {
		MdcSchedulingConfigurer configurer = new MdcSchedulingConfigurer();
		configurer.setApplicationContext(this.applicationContext);

		Object wrapped = Objects
			.requireNonNull(configurer.postProcessAfterInitialization(this.scheduler, "taskScheduler"));
		assertThat(wrapped).isInstanceOf(TaskScheduler.class);
		TaskScheduler taskScheduler = (TaskScheduler) wrapped;

		AtomicReference<String> traceId1 = new AtomicReference<>();
		AtomicReference<String> traceId2 = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(2);

		taskScheduler.schedule(() -> {
			traceId1.set(MDC.get(TRACE_ID));
			latch.countDown();
		}, new ImmediateTrigger());

		Thread.sleep(50);

		taskScheduler.schedule(() -> {
			traceId2.set(MDC.get(TRACE_ID));
			latch.countDown();
		}, new ImmediateTrigger());

		assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

		assertThat(traceId1.get()).isNotNull();
		assertThat(traceId2.get()).isNotNull();
	}

	static class FixedTraceIdGenerator implements TraceIdGenerator {

		@Override
		public String generate() {
			return "fixed-trace-id";
		}

	}

	static class ImmediateTrigger implements Trigger {

		private volatile boolean executed = false;

		@Override
		public Instant nextExecution(@NonNull TriggerContext triggerContext) {
			if (!this.executed) {
				this.executed = true;
				return Instant.now();
			}
			return null;
		}

	}

}
