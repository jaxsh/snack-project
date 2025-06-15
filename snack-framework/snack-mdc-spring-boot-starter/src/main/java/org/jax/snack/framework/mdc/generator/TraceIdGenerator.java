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

package org.jax.snack.framework.mdc.generator;

/**
 * Trace ID 生成器接口.
 * <p>
 * 实现此接口以提供自定义的 traceId 生成逻辑.
 *
 * @author Jax Jiang
 * @since 2025-06-15
 */
public interface TraceIdGenerator {

	/**
	 * 生成一个新的 traceId.
	 * @return 一个唯一的 traceId 字符串.
	 */
	String generate();

}
