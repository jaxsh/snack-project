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

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysDictDataDTO;
import org.jax.snack.upms.api.service.SysDictDataService;
import org.jax.snack.upms.api.vo.SysDictDataVO;
import org.jax.snack.upms.biz.converter.SysDictDataConverter;
import org.jax.snack.upms.biz.entity.SysDictData;
import org.jax.snack.upms.biz.entity.SysDictType;
import org.jax.snack.upms.biz.repository.SysDictDataRepository;
import org.jax.snack.upms.biz.repository.SysDictTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 字典数据服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysDictDataServiceImpl implements SysDictDataService {

	private final SysDictDataRepository repository;

	private final SysDictTypeRepository typeRepository;

	private final SysDictDataConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysDictDataDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysDictData.Fields.dictType, dto.getDictType())
			.eq(SysDictData.Fields.dictValue, dto.getDictValue())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Dictionary Data");
		}

		SysDictData entity = this.converter.toEntity(dto);
		this.repository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysDictDataDTO dto) {
		this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Dictionary Data"));

		SysDictData entity = this.converter.toEntity(dto);
		entity.setId(id);

		this.repository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		this.repository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysDictDataVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<SysDictDataVO> getByDictType(String dictType) {
		QueryCondition typeCondition = QueryCondition.builder()
			.eq(SysDictType.Fields.dictType, dictType)
			.eq(SysDictType.Fields.status, Status.ENABLED.getCode())
			.build();
		List<SysDictType> types = this.typeRepository.queryListByDsl(typeCondition);
		if (types.isEmpty()) {
			return List.of();
		}

		QueryCondition dataCondition = QueryCondition.builder()
			.eq(SysDictData.Fields.dictType, dictType)
			.eq(SysDictData.Fields.status, Status.ENABLED.getCode())
			.orderByAsc(SysDictData.Fields.sortOrder)
			.build();

		List<SysDictData> dataList = this.repository.queryListByDsl(dataCondition);
		return dataList.stream().map(this.converter::toVO).toList();
	}

}
