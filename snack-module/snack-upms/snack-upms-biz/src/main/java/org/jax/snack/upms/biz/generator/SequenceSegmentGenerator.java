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

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.biz.enums.SegmentType;
import org.jax.snack.upms.biz.repository.SysIdSequenceRepository;

import org.springframework.stereotype.Component;

/**
 * 序列号片段生成器.
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class SequenceSegmentGenerator implements SegmentGenerator {

	private final SysIdSequenceRepository sequenceRepository;

	@Override
	public void validate(Map<String, Object> config) {
		Object lengthValue = config.get("length");
		if (!(lengthValue instanceof Integer length)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "SEQUENCE segment requires 'length' as integer");
		}
		if (length <= 0) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "SEQUENCE length must be greater than 0");
		}
	}

	@Override
	public String generate(Map<String, Object> config, GeneratorContext context) {
		int length = (Integer) config.get("length");
		long nextValue = this.sequenceRepository.getNextValue(context.getRule().getId(), context.getCycleKey());
		return String.format("%0" + length + "d", nextValue);
	}

	@Override
	public SegmentType getType() {
		return SegmentType.SEQUENCE;
	}

}
