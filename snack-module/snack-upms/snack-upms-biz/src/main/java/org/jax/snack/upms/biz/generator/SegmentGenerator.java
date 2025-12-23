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

package org.jax.snack.upms.biz.generator;

import java.util.Map;

import org.jax.snack.upms.biz.enums.SegmentType;

/**
 * 片段生成器策略接口.
 *
 * @author Jax Jiang
 */
public interface SegmentGenerator {

	/**
	 * 校验片段配置.
	 * @param config 片段配置
	 * @throws IllegalArgumentException 配置无效时抛出
	 */
	void validate(Map<String, Object> config);

	/**
	 * 生成片段内容.
	 * @param config 片段配置
	 * @param context 上下文
	 * @return 生成的字符串
	 */
	String generate(Map<String, Object> config, GeneratorContext context);

	/**
	 * 获取支持的片段类型.
	 * @return 片段类型
	 */
	SegmentType getType();

}
