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
import java.util.random.RandomGenerator;

import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.biz.enums.SegmentType;

import org.springframework.stereotype.Component;

/**
 * 随机数字片段生成器.
 *
 * @author Jax Jiang
 */
@Component
public class RandomSegmentGenerator implements SegmentGenerator {

	private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

	@Override
	public void validate(Map<String, Object> config) {
		Object lengthValue = config.get("length");
		if (!(lengthValue instanceof Integer length)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "RANDOM segment requires 'length' as integer");
		}
		if (length <= 0) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "RANDOM length must be greater than 0");
		}
	}

	@Override
	public String generate(Map<String, Object> config, GeneratorContext context) {
		int length = (Integer) config.get("length");
		long max = (long) Math.pow(10, length);
		return String.format("%0" + length + "d", RANDOM.nextLong(max));
	}

	@Override
	public SegmentType getType() {
		return SegmentType.RANDOM;
	}

}
