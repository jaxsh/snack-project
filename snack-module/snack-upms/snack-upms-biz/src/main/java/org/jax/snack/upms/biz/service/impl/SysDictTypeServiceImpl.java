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
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.service.SysDictTypeService;
import org.jax.snack.upms.api.vo.SysDictTypeVO;
import org.jax.snack.upms.biz.converter.SysDictTypeConverter;
import org.jax.snack.upms.biz.entity.SysDictData;
import org.jax.snack.upms.biz.entity.SysDictType;
import org.jax.snack.upms.biz.repository.SysDictDataRepository;
import org.jax.snack.upms.biz.repository.SysDictTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 字典类型服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

	private final SysDictTypeRepository repository;

	private final SysDictDataRepository dictDataRepository;

	private final SysDictTypeConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysDictTypeDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysDictType.Fields.dictType, dto.getDictType())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Dictionary Type");
		}

		SysDictType entity = this.converter.toEntity(dto);
		this.repository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysDictTypeDTO dto) {
		this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Dictionary Type"));

		SysDictType entity = this.converter.toEntity(dto);
		entity.setId(id);

		this.repository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		this.repository.queryListByDsl(queryCondition).forEach((entity) -> {
			WhereCondition dataCondition = WhereCondition.builder()
				.eq(SysDictData.Fields.dictType, entity.getDictType())
				.build();
			this.dictDataRepository.deleteByDsl(dataCondition);
		});

		this.repository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysDictTypeVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

}
