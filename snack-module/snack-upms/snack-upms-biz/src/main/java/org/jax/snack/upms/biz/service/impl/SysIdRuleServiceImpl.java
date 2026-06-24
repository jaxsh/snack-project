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

package org.jax.snack.upms.biz.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.dto.SysIdRuleSegmentDTO;
import org.jax.snack.upms.api.enums.ResetCycle;
import org.jax.snack.upms.api.enums.SegmentType;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.api.vo.SysIdRuleVO;
import org.jax.snack.upms.biz.converter.SysIdRuleConverter;
import org.jax.snack.upms.biz.entity.SysIdRule;
import org.jax.snack.upms.biz.entity.SysIdSequence;
import org.jax.snack.upms.biz.generator.GeneratorContext;
import org.jax.snack.upms.biz.generator.SegmentGenerator;
import org.jax.snack.upms.biz.repository.SysIdRuleRepository;
import org.jax.snack.upms.biz.repository.SysIdSequenceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * ID 规则服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysIdRuleServiceImpl implements SysIdRuleService {

	private final SysIdRuleRepository ruleRepository;

	private final SysIdSequenceRepository sequenceRepository;

	private final SysIdRuleConverter converter;

	private final List<SegmentGenerator> segmentGenerators;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysIdRuleDTO dto) {
		validateSegments(dto.getSegments());

		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysIdRule.Fields.ruleCode, dto.getRuleCode())
			.build();
		if (this.ruleRepository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "ID Rule");
		}

		SysIdRule entity = this.converter.toEntity(dto);
		this.ruleRepository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysIdRuleDTO dto) {
		validateSegments(dto.getSegments());

		this.ruleRepository.findById(id).orElseThrow(this::notFound);

		SysIdRule entity = this.converter.toEntity(dto);
		entity.setId(id);
		this.ruleRepository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		this.ruleRepository.queryListByDsl(queryCondition).forEach((rule) -> {
			WhereCondition seqCondition = WhereCondition.builder()
				.eq(SysIdSequence.Fields.ruleId, rule.getId())
				.build();
			this.sequenceRepository.deleteByDsl(seqCondition);
		});

		this.ruleRepository.deleteByDsl(condition);
	}

	@Override
	public SysIdRuleVO getById(Long id) {
		SysIdRule rule = this.ruleRepository.findById(id).orElseThrow(this::notFound);
		return this.converter.toVO(rule);
	}

	@Override
	public PageResult<SysIdRuleVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.ruleRepository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.ruleRepository.queryListByDsl(condition));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String generate(String ruleCode) {
		return generate(ruleCode, null);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String generate(String ruleCode, Map<String, String> args) {
		SysIdRule rule = findByRuleCode(ruleCode).orElseThrow(this::notFound);

		List<SysIdRuleSegmentDTO> segments = rule.getSegments();
		if (CollectionUtils.isEmpty(segments)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "ID Rule has no segments configuration");
		}

		LocalDate currentDate = LocalDate.now();
		String baseCycleKey = ResetCycle.of(rule.getResetCycle()).computeCycleKey(currentDate);
		String shortName = Optional.ofNullable(args).map((m) -> m.get("shortName")).orElse(null);
		String cycleKey = StringUtils.hasText(shortName) ? baseCycleKey + "-" + shortName : baseCycleKey;
		GeneratorContext context = new GeneratorContext(rule, currentDate, cycleKey, args);

		StringBuilder result = new StringBuilder();
		for (SysIdRuleSegmentDTO segment : segments) {
			SegmentType type = SegmentType.of(segment.getSegmentType());
			SegmentGenerator generator = findGenerator(type);
			String segmentValue = generator.generate(segment.getConfig(), context);
			result.append(segmentValue);
		}

		return result.toString();
	}

	private void validateSegments(List<SysIdRuleSegmentDTO> segments) {
		if (ObjectUtils.isEmpty(segments)) {
			return;
		}
		for (SysIdRuleSegmentDTO segment : segments) {
			SegmentType type;
			try {
				type = SegmentType.of(segment.getSegmentType());
			}
			catch (IllegalArgumentException ex) {
				throw new BusinessException(ErrorCode.PARAM_INVALID,
						"Unknown segment type: " + segment.getSegmentType());
			}
			SegmentGenerator generator = findGenerator(type);
			generator.validate(segment.getConfig());
		}
	}

	private SegmentGenerator findGenerator(SegmentType type) {
		return this.segmentGenerators.stream()
			.filter((g) -> g.getType() == type)
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.PARAM_INVALID, "Unsupported segment type: " + type));
	}

	private Optional<SysIdRule> findByRuleCode(String ruleCode) {
		QueryCondition condition = QueryCondition.builder().eq(SysIdRule.Fields.ruleCode, ruleCode).build();
		List<SysIdRule> result = this.ruleRepository.queryListByDsl(condition);
		return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
	}

	private BusinessException notFound() {
		return new BusinessException(ErrorCode.DATA_NOT_FOUND, "ID Rule");
	}

}
