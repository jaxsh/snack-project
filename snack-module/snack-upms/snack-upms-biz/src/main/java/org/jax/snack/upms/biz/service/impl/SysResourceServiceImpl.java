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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.UpdateCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.utils.tree.TreeBuilder;
import org.jax.snack.framework.utils.tree.TreeNode;
import org.jax.snack.upms.api.dto.SysResourceDTO;
import org.jax.snack.upms.api.service.SysResourceService;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.biz.converter.SysResourceConverter;
import org.jax.snack.upms.biz.entity.SysResource;
import org.jax.snack.upms.biz.entity.SysRoleResource;
import org.jax.snack.upms.biz.repository.SysResourceRepository;
import org.jax.snack.upms.biz.repository.SysRoleResourceRepository;
import org.jax.snack.upms.biz.security.UpmsSecurityMetadataManager;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ObjectUtils;

/**
 * 资源服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl implements SysResourceService {

	private static final String CACHE_NAME = "upms:user";

	private final SysResourceRepository repository;

	private final SysRoleResourceRepository roleResourceRepository;

	private final SysResourceConverter converter;

	private final UpmsSecurityMetadataManager metadataManager;

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void create(SysResourceDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysResource.Fields.name, dto.getName())
			.eq(SysResource.Fields.parentId, dto.getParentId())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Resource");
		}

		if (dto.getParentId() != null && dto.getParentId() > 0) {
			this.repository.findById(dto.getParentId())
				.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Parent Resource"));
		}

		SysResource entity = this.converter.toEntity(dto);
		this.repository.save(entity);
		refreshPermissionRulesAfterCommit();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void update(Long id, SysResourceDTO dto) {
		this.repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Resource"));

		if (Objects.equals(id, dto.getParentId())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Parent cannot be self");
		}

		if (dto.getParentId() != null && dto.getParentId() > 0) {
			Set<Long> descendants = getDescendantIds(id);
			if (descendants.contains(dto.getParentId())) {
				throw new BusinessException(ErrorCode.PARAM_INVALID, "Parent cannot be a descendant");
			}
		}

		SysResource updateEntity = this.converter.toEntity(dto);
		updateEntity.setId(id);
		this.repository.update(updateEntity);

		if (Objects.equals(Status.DISABLED.getCode(), dto.getStatus())) {
			Set<Long> descendants = getDescendantIds(id);
			if (!descendants.isEmpty()) {
				WhereCondition where = WhereCondition.builder()
					.in(SysResource.Fields.id, new ArrayList<>(descendants))
					.build();
				SysResource updateParams = new SysResource();
				updateParams.setStatus(Status.DISABLED.getCode());
				this.repository.updateByDsl(updateParams, UpdateCondition.builder().where(where).build());
			}
		}
		refreshPermissionRulesAfterCommit();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = CACHE_NAME, allEntries = true)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		List<Long> targetIds = this.repository.queryListByDsl(queryCondition).stream().map(SysResource::getId).toList();
		if (targetIds.isEmpty()) {
			return;
		}

		List<SysResource> all = this.repository.queryListByDsl(null);
		List<TreeNode<SysResource>> tree = TreeBuilder.build(all, 0L, SysResource::getId, SysResource::getParentId);
		List<Long> allIds = new ArrayList<>(
				TreeBuilder.findDescendants(tree, SysResource::getId, targetIds.toArray(new Long[0])));

		this.roleResourceRepository
			.deleteByDsl(WhereCondition.builder().in(SysRoleResource.Fields.resourceId, allIds).build());
		this.repository.deleteByDsl(WhereCondition.builder().in(SysResource.Fields.id, allIds).build());

		refreshPermissionRulesAfterCommit();
	}

	private void refreshPermissionRulesAfterCommit() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					SysResourceServiceImpl.this.metadataManager.refresh();
				}
			});
		}
		else {
			this.metadataManager.refresh();
		}
	}

	private Set<Long> getDescendantIds(Long parentId) {
		List<SysResource> all = this.repository.queryListByDsl(null);
		List<TreeNode<SysResource>> tree = TreeBuilder.build(all, 0L, SysResource::getId, SysResource::getParentId);
		Set<Long> result = TreeBuilder.findDescendants(tree, SysResource::getId, parentId);
		result.remove(parentId);
		return result;
	}

	@Override
	public PageResult<SysResourceVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<TreeNode<SysResourceVO>> buildTree() {
		QueryCondition condition = QueryCondition.builder().orderByAsc(SysResource.Fields.sortOrder).build();
		List<SysResource> all = this.repository.queryListByDsl(condition);
		List<SysResourceVO> voList = all.stream().map(this.converter::toVO).toList();
		return TreeBuilder.build(voList, 0L, SysResourceVO::getId, SysResourceVO::getParentId);
	}

	@Override
	public List<SysResourceVO> findResourcesByRoleCode(String roleCode, Integer status) {
		return this.repository.selectResourcesByRoleCodes(List.of(roleCode), status)
			.stream()
			.map(this.converter::toVO)
			.toList();
	}

	@Override
	@Cacheable(value = CACHE_NAME, key = "'resources:role:' + #roleCode")
	public List<SysResourceVO> getResourcesByRoleCode(String roleCode) {
		return this.repository.selectResourcesByRoleCodes(List.of(roleCode), Status.ENABLED.getCode())
			.stream()
			.map(this.converter::toVO)
			.toList();
	}

	@Override
	public List<SysResourceVO> getEnabledResourcesByUsername(String username) {
		return this.repository.selectResourcesByUsername(username, Status.ENABLED.getCode())
			.stream()
			.map(this.converter::toVO)
			.toList();
	}

}
