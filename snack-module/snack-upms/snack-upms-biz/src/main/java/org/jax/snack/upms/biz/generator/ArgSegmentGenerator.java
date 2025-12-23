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

import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.biz.enums.SegmentType;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 动态参数片段生成器.
 * <p>
 * 从调用方传入的参数中获取值.
 *
 * @author Jax Jiang
 */
@Component
public class ArgSegmentGenerator implements SegmentGenerator {

	@Override
	public void validate(Map<String, Object> config) {
		Object keyValue = config.get("key");
		if (keyValue == null || !StringUtils.hasText(keyValue.toString())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "ARG segment requires non-empty 'key' config");
		}
	}

	@Override
	public String generate(Map<String, Object> config, GeneratorContext context) {
		String argKey = config.get("key").toString();
		Map<String, String> args = context.getArgs();
		if (args == null || !args.containsKey(argKey)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "ARG '" + argKey + "' is required but not provided");
		}
		return args.get(argKey);
	}

	@Override
	public SegmentType getType() {
		return SegmentType.ARG;
	}

}
