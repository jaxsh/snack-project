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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysMessageTemplateDTO;
import org.jax.snack.upms.api.service.SysMessageTemplateService;
import org.jax.snack.upms.api.vo.SysMessageTemplateVO;
import org.jax.snack.upms.biz.converter.SysMessageTemplateConverter;
import org.jax.snack.upms.biz.entity.SysMessageTemplate;
import org.jax.snack.upms.biz.repository.SysMessageTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 消息模版 Service 实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMessageTemplateServiceImpl implements SysMessageTemplateService {

	private final SysMessageTemplateRepository repository;

	private final SysMessageTemplateConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysMessageTemplateDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysMessageTemplate.Fields.templateCode, dto.getTemplateCode())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Template Code");
		}

		SysMessageTemplate entity = this.converter.toEntity(dto);
		this.repository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysMessageTemplateDTO dto) {
		this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Message Template"));

		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysMessageTemplate.Fields.templateCode, dto.getTemplateCode())
			.ne(SysMessageTemplate.Fields.id, id)
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Template Code");
		}

		SysMessageTemplate entity = this.converter.toEntity(dto);
		entity.setId(id);
		this.repository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		this.repository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysMessageTemplateVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public SysMessageTemplateVO getById(Long id) {
		return this.converter.toVO(this.repository.findById(id).orElse(null));
	}

}
