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
 * 固定字符串片段生成器.
 *
 * @author Jax Jiang
 */
@Component
public class FixedSegmentGenerator implements SegmentGenerator {

	@Override
	public void validate(Map<String, Object> config) {
		Object value = config.get("value");
		if (value == null || !StringUtils.hasText(value.toString())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "FIXED segment requires non-empty 'value' config");
		}
	}

	@Override
	public String generate(Map<String, Object> config, GeneratorContext context) {
		return config.get("value").toString();
	}

	@Override
	public SegmentType getType() {
		return SegmentType.FIXED;
	}

}
