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
 * 采用装饰器模式包装标准的 {@link Executor}, 实现 Trace ID 的跨线程传递.
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
