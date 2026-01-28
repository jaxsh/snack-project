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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.dto.SysRoleDTO;
import org.jax.snack.upms.api.service.SysRoleService;
import org.jax.snack.upms.api.vo.SysRoleVO;
import org.jax.snack.upms.biz.converter.SysRoleConverter;
import org.jax.snack.upms.biz.entity.SysRole;
import org.jax.snack.upms.biz.entity.SysRoleResource;
import org.jax.snack.upms.biz.entity.SysUserRole;
import org.jax.snack.upms.biz.repository.SysRoleRepository;
import org.jax.snack.upms.biz.repository.SysRoleResourceRepository;
import org.jax.snack.upms.biz.repository.SysUserRoleRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * 角色服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

	private final SysRoleRepository repository;

	private final SysRoleResourceRepository roleResourceRepository;

	private final SysUserRoleRepository userRoleRepository;

	private final SysRoleConverter converter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void create(SysRoleDTO dto) {
		QueryCondition condition = QueryCondition.builder().eq(SysRole.Fields.roleCode, dto.getRoleCode()).build();
		if (this.repository.existsByDsl(condition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Role Code");
		}

		SysRole entity = this.converter.toEntity(dto);
		this.repository.save(entity);

		if (!CollectionUtils.isEmpty(dto.getResourceIds())) {
			Set<SysRoleResource> relations = dto.getResourceIds().stream().map((resourceId) -> {
				SysRoleResource relation = new SysRoleResource();
				relation.setRoleCode(dto.getRoleCode());
				relation.setResourceId(resourceId);
				return relation;
			}).collect(Collectors.toSet());
			this.roleResourceRepository.saveBatch(relations);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void update(Long id, SysRoleDTO dto) {
		SysRole current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Role"));

		if (!current.getRoleCode().equals(dto.getRoleCode())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Role Code cannot be changed");
		}

		SysRole entity = this.converter.toEntity(dto);
		entity.setId(id);
		this.repository.update(entity);

		Optional.ofNullable(dto.getResourceIds()).ifPresent((resourceIds) -> {
			deleteRoleResourcesByRoleCode(current.getRoleCode());
			if (!CollectionUtils.isEmpty(resourceIds)) {
				Set<SysRoleResource> relations = resourceIds.stream().map((resourceId) -> {
					SysRoleResource relation = new SysRoleResource();
					relation.setRoleCode(current.getRoleCode());
					relation.setResourceId(resourceId);
					return relation;
				}).collect(Collectors.toSet());
				this.roleResourceRepository.saveBatch(relations);
			}
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		this.repository.queryListByDsl(queryCondition).forEach((entity) -> {
			String roleCode = entity.getRoleCode();
			deleteRoleResourcesByRoleCode(roleCode);
			deleteUserRolesByRoleCode(roleCode);
		});

		this.repository.deleteByDsl(condition);
	}

	private void deleteRoleResourcesByRoleCode(String roleCode) {
		WhereCondition condition = WhereCondition.builder().eq(SysRoleResource.Fields.roleCode, roleCode).build();
		this.roleResourceRepository.deleteByDsl(condition);
	}

	private void deleteUserRolesByRoleCode(String roleCode) {
		WhereCondition condition = WhereCondition.builder().eq(SysUserRole.Fields.roleCode, roleCode).build();
		this.userRoleRepository.deleteByDsl(condition);
	}

	@Override
	public PageResult<SysRoleVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

}
