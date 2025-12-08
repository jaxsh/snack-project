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
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.OrderByCondition;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.query.SortDirection;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysDictDataDTO;
import org.jax.snack.upms.api.enums.Status;
import org.jax.snack.upms.api.service.SysDictDataService;
import org.jax.snack.upms.api.vo.SysDictDataVO;
import org.jax.snack.upms.biz.converter.SysDictDataConverter;
import org.jax.snack.upms.biz.entity.SysDictData;
import org.jax.snack.upms.biz.entity.SysDictType;
import org.jax.snack.upms.biz.repository.SysDictDataRepository;
import org.jax.snack.upms.biz.repository.SysDictTypeRepository;

import org.springframework.stereotype.Service;

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
	public void create(SysDictDataDTO dto) {
		QueryCondition existsCondition = new QueryCondition();
		existsCondition.setWhere(Map.of("dictType", Map.of(QueryOperator.EQ.getValue(), dto.getDictType()), "dictValue",
				Map.of(QueryOperator.EQ.getValue(), dto.getDictValue())));
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Dictionary Data");
		}

		SysDictData entity = this.converter.toEntity(dto);
		this.repository.save(entity);
	}

	@Override
	public void update(Long id, SysDictDataDTO dto) {
		this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Dictionary Data"));

		SysDictData entity = this.converter.toEntity(dto);
		entity.setId(id);

		this.repository.update(entity);
	}

	@Override
	public void deleteById(Long id) {
		this.repository.deleteById(id);
	}

	@Override
	public PageResult<SysDictDataVO> queryByDsl(QueryCondition condition) {
		if (condition.getSize() != null && condition.getSize() > 0) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<SysDictDataVO> getByDictType(String dictType) {
		QueryCondition typeCondition = new QueryCondition();
		typeCondition.setWhere(Map.of("dictType", Map.of(QueryOperator.EQ.getValue(), dictType), "status",
				Map.of(QueryOperator.EQ.getValue(), Status.ENABLED.getCode())));
		List<SysDictType> types = this.typeRepository.queryListByDsl(typeCondition);
		if (types.isEmpty()) {
			return List.of();
		}

		QueryCondition dataCondition = new QueryCondition();
		dataCondition.setWhere(Map.of("dictType", Map.of(QueryOperator.EQ.getValue(), dictType), "status",
				Map.of(QueryOperator.EQ.getValue(), Status.ENABLED.getCode())));
		OrderByCondition orderBy = new OrderByCondition();
		orderBy.setField("sortOrder");
		orderBy.setDirection(SortDirection.ASC.getValue());
		dataCondition.setOrderBy(List.of(orderBy));

		List<SysDictData> dataList = this.repository.queryListByDsl(dataCondition);
		return dataList.stream().map(this.converter::toVO).toList();
	}

	@Override
	public void saveBatch(List<SysDictDataDTO> dtoList) {
		List<SysDictData> entities = dtoList.stream().map(this.converter::toEntity).toList();
		this.repository.saveOrUpdateBatch(entities);
	}

}
