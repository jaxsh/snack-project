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

import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.MDC;

import org.springframework.lang.NonNull;

/**
 * 一个用于包装现有 {@link Executor} 的装饰器, 为其添加 MDC 上下文传播能力.
 * <p>
 * 它遵循 "捕获与恢复" 模式, 会捕获提交任务时所在线程的 MDC 上下文, 并在任务真正执行时, 将该上下文恢复到执行线程中. 这对于确保
 * {@code CompletableFuture} 等异步操作的日志中包含正确的 traceId至关重要.
 *
 * @author Jax Jiang
 * @since 2025-06-09
 */
public class MdcAwareExecutor implements Executor {

	/**
	 * 被包装的原始 Executor 实例. 真正的任务执行将委托给它.
	 */
	private final Executor delegate;

	/**
	 * 构造一个新的 MdcAwareExecutor.
	 * @param delegate 需要被包装以添加 MDC 功能的原始 Executor 实例.
	 */
	public MdcAwareExecutor(Executor delegate) {
		this.delegate = delegate;
	}

	/**
	 * 执行一个被 MDC 上下文包装过的任务.
	 * <p>
	 * 这是实现 MDC 传播的核心方法.
	 * @param command 原始的、由调用者提交的 Runnable 任务.
	 */
	@Override
	public void execute(@NonNull Runnable command) {
		// 1. 在父线程 (调用 execute 方法的线程) 中, 捕获当前的 MDC 上下文快照.
		final Map<String, String> contextMap = MDC.getCopyOfContextMap();

		// 2. 创建一个新的 Runnable 包装器.
		// 这个包装器将在子线程 (执行任务的线程) 中运行.
		Runnable wrappedCommand = () -> {
			try {
				// 3. 在任务执行前, 将之前捕获的 MDC 上下文恢复到当前子线程.
				if (contextMap != null) {
					MDC.setContextMap(contextMap);
				}

				// 4. 执行真正的、原始的任务逻辑.
				command.run();
			}
			finally {
				// 5. 无论任务成功还是失败, 在 finally 块中必须清理当前子线程的 MDC.
				// 这可以防止线程池中的线程被"污染", 影响后续使用该线程的其他任务.
				MDC.clear();
			}
		};

		// 6. 将我们包装过的任务, 提交给原始的 Executor 去执行.
		this.delegate.execute(wrappedCommand);
	}

}
