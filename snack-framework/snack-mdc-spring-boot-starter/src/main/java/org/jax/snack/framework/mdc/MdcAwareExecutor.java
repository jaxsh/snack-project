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

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

/**
 * 支持 MDC 传递的 Executor 装饰器.
 * <p>
 * 采用装饰器模式包装标准的 {@link Executor}，实现 Trace ID 的跨线程传递. 通常用于 {@code CompletableFuture}
 * 或手动提交任务的场景.
 * <p>
 * <b>工作原理（捕获与恢复模式）：</b>
 * <ol>
 * <li><b>Capture：</b> 在任务提交时（父线程），捕获当前的 MDC 上下文快照.</li>
 * <li><b>Restore：</b> 在任务执行时（子线程），将捕获的上下文恢复到当前线程.</li>
 * <li><b>Clear：</b> 任务结束后，强制清理子线程的 MDC，防止线程污染.</li>
 * </ol>
 *
 * @author Jax Jiang
 */
public class MdcAwareExecutor implements Executor {

	/**
	 * 被包装的原始执行器.
	 */
	private final Executor delegate;

	/**
	 * 构造函数.
	 * @param delegate 被委托的原始执行器
	 */
	public MdcAwareExecutor(Executor delegate) {
		this.delegate = delegate;
	}

	/**
	 * 提交并执行任务.
	 * @param command 原始任务
	 */
	@Override
	public void execute(@NonNull Runnable command) {
		final Map<String, String> contextMap = MDC.getCopyOfContextMap();

		Runnable wrappedCommand = () -> {
			try {
				if (contextMap != null) {
					MDC.setContextMap(contextMap);
				}
				command.run();
			}
			finally {
				MDC.clear();
			}
		};

		this.delegate.execute(wrappedCommand);
	}

}
