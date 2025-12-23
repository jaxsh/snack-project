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

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.biz.enums.SegmentType;

import org.springframework.stereotype.Component;

/**
 * 日期时间片段生成器.
 *
 * @author Jax Jiang
 */
@Component
public class DateSegmentGenerator implements SegmentGenerator {

	@Override
	public void validate(Map<String, Object> config) {
		Object patternValue = config.get("pattern");
		if (patternValue == null) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "DATE segment requires 'pattern' config");
		}
		String pattern = patternValue.toString();
		try {
			DateTimeFormatter.ofPattern(pattern);
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Invalid date pattern: " + pattern);
		}
	}

	@Override
	public String generate(Map<String, Object> config, GeneratorContext context) {
		String pattern = config.get("pattern").toString();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
		return context.getCurrentDate().format(formatter);
	}

	@Override
	public SegmentType getType() {
		return SegmentType.DATE;
	}

}
