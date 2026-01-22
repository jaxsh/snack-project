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

package org.jax.snack.lowcode.biz.service.schema;

import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.enums.SchemaStatus;
import org.jax.snack.lowcode.api.service.LowcodeSchemaService;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.converter.LowcodeSchemaConverter;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.jax.snack.lowcode.biz.entity.LowcodeSchemaHistory;
import org.jax.snack.lowcode.biz.model.SchemaChange;
import org.jax.snack.lowcode.biz.repository.LowcodeSchemaHistoryRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeSchemaRepository;
import org.jax.snack.lowcode.biz.service.sync.TableSyncExecutor;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * Schema 管理服务实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeSchemaServiceImpl implements LowcodeSchemaService {

	private final LowcodeSchemaHistoryRepository historyRepository;

	private final LowcodeSchemaRepository schemaRepository;

	private final LowcodeSchemaConverter schemaConverter;

	private final SchemaService schemaService;

	private final SchemaAssembler schemaAssembler;

	private final SchemaChangeDetector changeDetector;

	private final TableSyncExecutor tableSyncExecutor;

	@Override
	public PageResult<LowcodeSchemaVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.schemaConverter.toPageResult(this.schemaRepository.queryPageByDsl(condition));
		}
		else {
			return this.schemaConverter.toPageResult(this.schemaRepository.queryListByDsl(condition));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void create(LowcodeSchemaDTO dto) {
		QueryCondition existsCondition = QueryCondition.builder()
			.eq(LowcodeSchema.Fields.schemaName, dto.getSchemaName())
			.build();
		if (this.schemaRepository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Schema");
		}

		LowcodeSchema entity = this.schemaConverter.toEntity(dto);
		entity.setStatus(SchemaStatus.DRAFT.getCode());
		entity.setSchemaJson(this.schemaAssembler.toJsonSchema(dto));

		this.schemaRepository.save(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void update(Long id, LowcodeSchemaDTO dto) {
		this.schemaRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Schema"));

		LowcodeSchema entity = this.schemaConverter.toEntity(dto);
		entity.setId(id);
		entity.setSchemaJson(this.schemaAssembler.toJsonSchema(dto));

		this.schemaRepository.update(entity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		QueryCondition queryCondition = QueryCondition.builder().where(condition.getWhere()).build();
		this.schemaRepository.queryListByDsl(queryCondition).forEach((schema) -> {
			this.schemaService.clearCache(schema.getSchemaName());
			log.info("Deleting Schema: {}", schema.getSchemaName());
		});

		this.schemaRepository.deleteByDsl(condition);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void publishSchema(Long id) {
		LowcodeSchema draft = this.schemaRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Schema"));

		if (!SchemaStatus.DRAFT.getCode().equals(draft.getStatus())) {
			throw new BusinessException(ErrorCode.DATA_STATUS_ERROR, "Only schemas in DRAFT status can be published");
		}

		LowcodeSchema published = findPublishedSchema(draft.getSchemaName());

		JsonNode baseJson = (published != null) ? published.getSchemaJson() : null;
		List<SchemaChange> changes = this.changeDetector.detectChanges(draft.getSchemaJson(), baseJson);

		this.tableSyncExecutor.syncTable(draft.getSchemaName(), draft.getSchemaJson(), changes);

		if (published != null) {
			this.schemaConverter.updateFromDraft(draft, published);
			this.schemaRepository.update(published);
		}
		else {
			published = this.schemaConverter.copyForPublish(draft);
			published.setStatus(SchemaStatus.PUBLISHED.getCode());
			this.schemaRepository.save(published);
		}

		saveHistory(published, changes);

		this.schemaService.clearCache(draft.getSchemaName());
	}

	private LowcodeSchema findPublishedSchema(String schemaName) {
		QueryCondition publishedCondition = QueryCondition.builder()
			.eq(LowcodeSchema.Fields.schemaName, schemaName)
			.eq(LowcodeSchema.Fields.status, SchemaStatus.PUBLISHED.getCode())
			.build();
		List<LowcodeSchema> publishedList = this.schemaRepository.queryListByDsl(publishedCondition);
		return publishedList.isEmpty() ? null : publishedList.get(0);
	}

	private void saveHistory(LowcodeSchema published, List<SchemaChange> changes) {
		String changeSummary = changes.stream().map(this::formatChange).collect(Collectors.joining(", "));

		LowcodeSchemaHistory history = new LowcodeSchemaHistory();
		history.setSchemaName(published.getSchemaName());
		history.setSchemaJson(published.getSchemaJson());
		history.setChangeSummary(changeSummary);
		this.historyRepository.save(history);
	}

	private String formatChange(SchemaChange c) {
		String type = c.getType().name();
		String col = c.getColumnName();
		return switch (c.getType()) {
			case ADD_TABLE -> type;
			case ADD_COLUMN, DROP_COLUMN -> type + ":" + col;
			case RENAME_COLUMN -> type + ":" + c.getOldValue() + "→" + c.getNewValue();
			case MODIFY_COLUMN_TYPE, MODIFY_LENGTH, MODIFY_DEFAULT ->
				type + ":" + col + ":" + c.getOldValue() + "→" + c.getNewValue();
			case MODIFY_NULLABLE ->
				type + ":" + col + ":" + (Boolean.TRUE.equals(c.getNullable()) ? "→NULL" : "→NOT NULL");
			case ADD_INDEX -> type + ":" + c.getIndexName() + "(" + String.join(",", c.getIndexColumns()) + ")";
			case DROP_INDEX -> type + ":" + c.getIndexName();
		};
	}

}
