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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
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

	private final SysResourceRepository repository;

	private final SysRoleResourceRepository roleResourceRepository;

	private final SysResourceConverter converter;

	private final UpmsSecurityMetadataManager metadataManager;

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void create(SysResourceDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(SysResource.Fields.name, dto.getName())
			.eq(SysResource.Fields.parentId, dto.getParentId())
			.build();
		if (this.repository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Resource");
		}

		if (!ObjectUtils.isEmpty(dto.getParentId()) && dto.getParentId() > 0) {
			this.repository.findById(dto.getParentId())
				.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Parent Resource"));
		}

		SysResource entity = this.converter.toEntity(dto);
		this.repository.save(entity);
		refreshPermissionRulesAfterCommit();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void update(Long id, SysResourceDTO dto) {
		this.repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Resource"));

		if (Objects.equals(id, dto.getParentId())) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Parent cannot be self");
		}

		SysResource updateEntity = this.converter.toEntity(dto);
		updateEntity.setId(id);
		this.repository.update(updateEntity);

		if (Objects.equals(Status.DISABLED.getCode(), dto.getStatus())) {
			Set<Long> descendantIds = getDescendantIds(id);
			if (!descendantIds.isEmpty()) {
				WhereCondition where = WhereCondition.builder()
					.in(SysResource.Fields.id, new ArrayList<>(descendantIds))
					.build();
				SysResource updateParams = new SysResource();
				updateParams.setStatus(Status.DISABLED.getCode());
				this.repository.updateByDsl(updateParams, where);
			}
		}
		refreshPermissionRulesAfterCommit();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@CacheEvict(value = "upms:user", allEntries = true)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		this.repository.queryListByDsl(queryCondition).forEach((entity) -> {
			Set<Long> descendantIds = getDescendantIds(entity.getId());
			descendantIds.add(entity.getId());

			WhereCondition roleResCondition = WhereCondition.builder()
				.in(SysRoleResource.Fields.resourceId, new ArrayList<>(descendantIds))
				.build();
			this.roleResourceRepository.deleteByDsl(roleResCondition);

			WhereCondition deleteCondition = WhereCondition.builder()
				.in(SysResource.Fields.id, new ArrayList<>(descendantIds))
				.build();
			this.repository.deleteByDsl(deleteCondition);
		});
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
		List<Long> collector = new ArrayList<>();
		List<SysResource> all = this.repository.queryListByDsl(null);
		recursiveFindChildren(parentId, all, collector);
		return new HashSet<>(collector);
	}

	private void recursiveFindChildren(Long parentId, List<SysResource> all, List<Long> collector) {
		for (SysResource res : all) {
			if (Objects.equals(res.getParentId(), parentId)) {
				collector.add(res.getId());
				recursiveFindChildren(res.getId(), all, collector);
			}
		}
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
		List<SysResource> all = this.repository.queryListByDsl(null);
		List<SysResourceVO> voList = all.stream().map(this.converter::toVO).toList();
		return TreeBuilder.build(voList, 0L, SysResourceVO::getId, SysResourceVO::getParentId);
	}

	@Override
	public List<SysResourceVO> findResourcesByRoleCode(String roleCode) {
		List<SysResource> resources = this.repository.selectResourcesByRoleCode(roleCode);
		return resources.stream().map(this.converter::toVO).collect(Collectors.toList());
	}

}
