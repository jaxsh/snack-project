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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.jax.snack.upms.api.dto.SysOrgDTO;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.api.service.SysOrgService;
import org.jax.snack.upms.api.vo.SysOrgLevelNameVO;
import org.jax.snack.upms.api.vo.SysOrgVO;
import org.jax.snack.upms.biz.converter.SysOrgConverter;
import org.jax.snack.upms.biz.converter.SysOrgLevelNameConverter;
import org.jax.snack.upms.biz.entity.SysOrg;
import org.jax.snack.upms.biz.entity.SysOrgLevelName;
import org.jax.snack.upms.biz.entity.SysUserOrg;
import org.jax.snack.upms.biz.repository.SysOrgLevelNameRepository;
import org.jax.snack.upms.biz.repository.SysOrgRepository;
import org.jax.snack.upms.biz.repository.SysUserOrgRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 组织机构服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysOrgServiceImpl implements SysOrgService {

	private static final String ANCESTORS_SEPARATOR = ",";

	private final SysOrgRepository repository;

	private final SysOrgLevelNameRepository levelNameRepository;

	private final SysUserOrgRepository userOrgRepository;

	private final SysIdRuleService idRuleService;

	private final SysOrgConverter converter;

	private final SysOrgLevelNameConverter levelNameConverter;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(SysOrgDTO dto) {
		SysOrg entity = this.converter.toEntity(dto);

		String orgCode = this.idRuleService.generate("org_code");
		entity.setOrgCode(orgCode);

		if (!StringUtils.hasText(dto.getParentCode())) {
			entity.setParentCode(null);
			entity.setLevel(0);
			entity.setAncestors("");
		}
		else {
			SysOrg parent = findByOrgCode(dto.getParentCode())
				.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Parent Organization"));

			entity.setLevel(parent.getLevel() + 1);
			entity.setAncestors(buildAncestors(parent));
		}

		this.repository.save(entity);

		if (!CollectionUtils.isEmpty(dto.getLevelNames()) && !StringUtils.hasText(dto.getParentCode())) {
			List<SysOrgLevelName> levelNames = this.levelNameConverter.toEntity(dto.getLevelNames());
			levelNames.forEach((ln) -> ln.setRootId(entity.getId()));
			this.levelNameRepository.saveBatch(levelNames);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, SysOrgDTO dto) {
		SysOrg current = this.repository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Organization"));

		SysOrg entity = this.converter.toEntity(dto);
		entity.setId(id);
		entity.setOrgCode(current.getOrgCode());

		String currentParentCode = current.getParentCode();
		String newParentCode = dto.getParentCode();

		if (StringUtils.hasText(newParentCode) && !newParentCode.equals(currentParentCode)) {
			SysOrg newParent = findByOrgCode(newParentCode)
				.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Parent Organization"));

			String oldPrefix = buildAncestorsPrefix(current);
			String newPrefix = buildAncestors(newParent) + ANCESTORS_SEPARATOR + current.getOrgCode();
			int levelDiff = (newParent.getLevel() + 1) - current.getLevel();

			this.repository.batchUpdateDescendants(oldPrefix, newPrefix, levelDiff);

			entity.setParentCode(newParentCode);
			entity.setLevel(newParent.getLevel() + 1);
			entity.setAncestors(buildAncestors(newParent));
		}
		else if ("".equals(newParentCode) && StringUtils.hasText(currentParentCode)) {
			String oldPrefix = buildAncestorsPrefix(current);
			int levelDiff = -current.getLevel();

			this.repository.batchUpdateDescendants(oldPrefix, current.getOrgCode(), levelDiff);

			SysOrg rootFields = new SysOrg();
			rootFields.setLevel(0);
			rootFields.setAncestors("");
			WhereCondition idWhere = WhereCondition.builder().eq(SysOrg.Fields.id, id).build();
			this.repository.updateByDsl(rootFields,
					UpdateCondition.builder().setNull(SysOrg.Fields.parentCode).where(idWhere).build());
		}

		if (Status.DISABLED.getCode().equals(dto.getStatus())) {
			String ancestorsPrefix = buildAncestorsPrefix(current);
			WhereCondition where = WhereCondition.builder().likeRight(SysOrg.Fields.ancestors, ancestorsPrefix).build();
			SysOrg updateParams = new SysOrg();
			updateParams.setStatus(Status.DISABLED.getCode());
			this.repository.updateByDsl(updateParams, UpdateCondition.builder().where(where).build());
		}

		this.repository.update(entity);

		if (!CollectionUtils.isEmpty(dto.getLevelNames()) && !StringUtils.hasText(dto.getParentCode())) {
			WhereCondition where = WhereCondition.builder().eq(SysOrgLevelName.Fields.rootId, id).build();
			this.levelNameRepository.deleteByDsl(where);
			List<SysOrgLevelName> levelNames = this.levelNameConverter.toEntity(dto.getLevelNames());
			levelNames.forEach((ln) -> ln.setRootId(id));
			this.levelNameRepository.saveBatch(levelNames);
		}

		if (!StringUtils.hasText(currentParentCode) && StringUtils.hasText(newParentCode)) {
			WhereCondition where = WhereCondition.builder().eq(SysOrgLevelName.Fields.rootId, id).build();
			this.levelNameRepository.deleteByDsl(where);
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		List<String> allOrgCodes = new ArrayList<>();

		this.repository.queryListByDsl(queryCondition).forEach((entity) -> {
			allOrgCodes.add(entity.getOrgCode());

			List<SysOrg> descendants = findByAncestorsPrefix(buildAncestorsPrefix(entity));
			if (!CollectionUtils.isEmpty(descendants)) {
				descendants.forEach((d) -> allOrgCodes.add(d.getOrgCode()));
				List<Long> descendantIds = descendants.stream().map(SysOrg::getId).toList();
				WhereCondition where = WhereCondition.builder().in(SysOrg.Fields.id, descendantIds).build();
				this.repository.deleteByDsl(where);
			}

			if (!StringUtils.hasText(entity.getParentCode())) {
				WhereCondition where = WhereCondition.builder()
					.eq(SysOrgLevelName.Fields.rootId, entity.getId())
					.build();
				this.levelNameRepository.deleteByDsl(where);
			}
		});

		this.repository.deleteByDsl(condition);

		if (!CollectionUtils.isEmpty(allOrgCodes)) {
			WhereCondition where = WhereCondition.builder().in(SysUserOrg.Fields.orgCode, allOrgCodes).build();
			this.userOrgRepository.deleteByDsl(where);
		}
	}

	@Override
	public PageResult<SysOrgVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	public List<TreeNode<SysOrgVO>> buildTree(String... orgCodes) {
		List<SysOrg> allOrgs = findAll();
		Map<String, SysOrg> orgByCode = allOrgs.stream().collect(Collectors.toMap(SysOrg::getOrgCode, (org) -> org));
		Set<Long> rootIds = allOrgs.stream()
			.filter((org) -> !StringUtils.hasText(org.getParentCode()))
			.map(SysOrg::getId)
			.collect(Collectors.toSet());
		Map<Long, Map<Integer, String>> levelNameMap = queryLevelNames(rootIds).stream()
			.collect(Collectors.groupingBy(SysOrgLevelName::getRootId,
					Collectors.toMap(SysOrgLevelName::getLevel, SysOrgLevelName::getLevelName)));
		List<SysOrgVO> voList = allOrgs.stream()
			.map((entity) -> toVoWithLevelName(entity, orgByCode, levelNameMap))
			.toList();

		return TreeBuilder.build(voList, null, SysOrgVO::getOrgCode, SysOrgVO::getParentCode, orgCodes);
	}

	@Override
	public Set<String> findDescendantOrgCodes(String... orgCodes) {
		Set<String> result = new HashSet<>();

		for (String orgCode : orgCodes) {
			findByOrgCode(orgCode).ifPresent((entity) -> {
				result.add(orgCode);
				String ancestorsPrefix = buildAncestorsPrefix(entity);
				List<SysOrg> descendants = findByAncestorsPrefix(ancestorsPrefix);
				descendants.forEach((org) -> result.add(org.getOrgCode()));
			});
		}

		return result;
	}

	@Override
	public List<SysOrgLevelNameVO> findLevelNames(Long rootId) {
		return this.levelNameConverter.toVO(queryLevelNames(Set.of(rootId)));
	}

	private Optional<SysOrg> findByOrgCode(String orgCode) {
		QueryCondition condition = QueryCondition.builder().eq(SysOrg.Fields.orgCode, orgCode).build();
		List<SysOrg> result = this.repository.queryListByDsl(condition);
		return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
	}

	private List<SysOrg> findAll() {
		return this.repository.queryListByDsl(QueryCondition.builder().orderByAsc(SysOrg.Fields.sortOrder).build());
	}

	private List<SysOrg> findByAncestorsPrefix(String ancestorsPrefix) {
		QueryCondition condition = QueryCondition.builder().likeRight(SysOrg.Fields.ancestors, ancestorsPrefix).build();
		return this.repository.queryListByDsl(condition);
	}

	private SysOrgVO toVoWithLevelName(SysOrg entity, Map<String, SysOrg> orgByCode,
			Map<Long, Map<Integer, String>> levelNameMap) {
		SysOrgVO vo = this.converter.toVO(entity);

		Optional.ofNullable(getRootOrgCode(entity))
			.map(orgByCode::get)
			.map((root) -> levelNameMap.getOrDefault(root.getId(), Map.of()).get(entity.getLevel()))
			.ifPresent(vo::setLevelName);

		return vo;
	}

	private List<SysOrgLevelName> queryLevelNames(Set<Long> rootIds) {
		if (CollectionUtils.isEmpty(rootIds)) {
			return List.of();
		}
		WhereCondition where = WhereCondition.builder().in(SysOrgLevelName.Fields.rootId, rootIds).build();
		QueryCondition condition = QueryCondition.builder().where(where.getWhere()).build();
		return this.levelNameRepository.queryListByDsl(condition);
	}

	private String buildAncestors(SysOrg parent) {
		if (StringUtils.hasText(parent.getAncestors())) {
			return parent.getAncestors() + ANCESTORS_SEPARATOR + parent.getOrgCode();
		}
		return parent.getOrgCode();
	}

	private String buildAncestorsPrefix(SysOrg entity) {
		if (StringUtils.hasText(entity.getAncestors())) {
			return entity.getAncestors() + ANCESTORS_SEPARATOR + entity.getOrgCode();
		}
		return entity.getOrgCode();
	}

	private String getRootOrgCode(SysOrg entity) {
		if (!StringUtils.hasText(entity.getAncestors())) {
			return entity.getOrgCode();
		}
		String[] parts = entity.getAncestors().split(ANCESTORS_SEPARATOR);
		return (parts.length > 0) ? parts[0] : null;
	}

}
