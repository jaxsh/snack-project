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

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import org.springframework.core.task.TaskDecorator;

/**
 * MDC 任务装饰器.
 * <p>
 * 实现 Spring 的 {@link TaskDecorator} 接口, 用于在线程池执行任务时传递 MDC 上下文.
 *
 * @author Jax Jiang
 */
public class MdcTaskDecorator implements TaskDecorator {

	/**
	 * 装饰 Runnable 任务.
	 * @param runnable 原始任务
	 * @return 包装了 MDC 上下文处理的新任务
	 */
	@Override
	@NonNull public Runnable decorate(@NonNull Runnable runnable) {
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
