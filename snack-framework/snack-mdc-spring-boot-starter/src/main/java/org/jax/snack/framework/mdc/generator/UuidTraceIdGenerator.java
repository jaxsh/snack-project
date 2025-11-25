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

import java.util.UUID;

/**
 * 基于 UUID 的 Trace ID 生成器默认实现.
 * <p>
 * 生成策略：UUID 去除横杠，并截取前 16 位.
 *
 * @author Jax Jiang
 */
public class UuidTraceIdGenerator implements TraceIdGenerator {

	private static final int TRACE_ID_LENGTH = 16;

	/**
	 * 生成 Trace ID.
	 * @return 16 位的 UUID 字符串
	 */
	@Override
	public String generate() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
	}

}
