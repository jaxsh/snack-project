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

import org.slf4j.MDC;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

/**
 * 一个用于将 MDC 上下文传播到异步任务执行器中线程的 {@link TaskDecorator}.
 * <p>
 * 这可以确保在异步任务中产生的日志, 能够包含发起该任务的父线程中的正确 traceId.
 *
 * @author Jax Jiang
 * @since 2025-06-09
 */
public class MdcTaskDecorator implements TaskDecorator {

	/**
	 * 装饰给定的 {@link Runnable} 任务, 包装它以管理 MDC 上下文.
	 * @param runnable 原始的待执行任务.
	 * @return 一个新的 {@link Runnable} 实例, 它将在父线程的 MDC 上下文中执行.
	 */
	@Override
	@NonNull
	public Runnable decorate(@NonNull Runnable runnable) {
		Map<String, String> contextMap = MDC.getCopyOfContextMap();
		return () -> {
			try {
				if (contextMap != null) {
					MDC.setContextMap(contextMap);
				}
				runnable.run();
			}
			finally {
				MDC.clear();
			}
		};
	}

}
