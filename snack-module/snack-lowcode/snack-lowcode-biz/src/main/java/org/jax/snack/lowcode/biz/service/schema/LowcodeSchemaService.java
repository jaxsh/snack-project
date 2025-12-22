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
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.enums.SchemaStatus;
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

/**
 * Schema 管理服务.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodeSchemaService {

	private final LowcodeSchemaHistoryRepository historyRepository;

	private final LowcodeSchemaRepository schemaRepository;

	private final LowcodeSchemaConverter schemaConverter;

	private final SchemaService schemaService;

	private final SchemaAssembler schemaAssembler;

	private final SchemaChangeDetector changeDetector;

	private final TableSyncExecutor tableSyncExecutor;

	/**
	 * 按条件查询 Schema.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	public PageResult<LowcodeSchemaVO> queryByDsl(QueryCondition condition) {
		if (condition.getSize() != null && condition.getSize() > 0) {
			return this.schemaConverter.toPageResult(this.schemaRepository.queryPageByDsl(condition));
		}
		else {
			return this.schemaConverter.toPageResult(this.schemaRepository.queryListByDsl(condition));
		}
	}

	/**
	 * 创建 Schema 草稿.
	 * @param dto Schema DTO
	 */
	@Transactional
	public void createSchema(LowcodeSchemaDTO dto) {
		QueryCondition existsCondition = new QueryCondition();
		existsCondition.setWhere(Map.of("schemaName", Map.of(QueryOperator.EQ.getValue(), dto.getSchemaName())));
		if (this.schemaRepository.existsByDsl(existsCondition)) {
			throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "Schema");
		}

		LowcodeSchema entity = this.schemaConverter.toEntity(dto);
		entity.setStatus(SchemaStatus.DRAFT.getCode());
		entity.setSchemaJson(this.schemaAssembler.toJsonSchema(dto));

		this.schemaRepository.save(entity);
	}

	/**
	 * 更新 Schema.
	 * @param id Schema ID
	 * @param dto Schema DTO
	 */
	@Transactional
	public void updateSchema(Long id, LowcodeSchemaDTO dto) {
		this.schemaRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Schema"));

		LowcodeSchema entity = this.schemaConverter.toEntity(dto);
		entity.setId(id);
		entity.setSchemaJson(this.schemaAssembler.toJsonSchema(dto));

		this.schemaRepository.update(entity);
	}

	/**
	 * 删除 Schema.
	 * @param id Schema ID
	 */
	@Transactional
	public void deleteById(Long id) {
		LowcodeSchema entity = this.schemaRepository.findById(id).orElse(null);
		if (entity != null) {
			this.schemaRepository.deleteById(id);
			this.schemaService.clearCache(entity.getSchemaName());
			log.info("删除 Schema: {}", entity.getSchemaName());
		}
	}

	/**
	 * 发布 Schema, 对比变更并同步到物理表.
	 * @param id Schema ID
	 */
	@Transactional
	public void publishSchema(Long id) {
		LowcodeSchema draft = this.schemaRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Schema"));

		if (!SchemaStatus.DRAFT.getCode().equals(draft.getStatus())) {
			throw new BusinessException(ErrorCode.DATA_STATUS_ERROR, "只有草稿状态的 Schema 才能发布");
		}

		// 查询已发布版本
		LowcodeSchema published = findPublishedSchema(draft.getSchemaName());

		JsonNode baseJson = (published != null) ? published.getSchemaJson() : null;
		List<SchemaChange> changes = this.changeDetector.detectChanges(draft.getSchemaJson(), baseJson);

		// 同步到物理数据库
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
		QueryCondition publishedCondition = new QueryCondition();
		publishedCondition.setWhere(Map.of("schemaName", Map.of(QueryOperator.EQ.getValue(), schemaName), "status",
				Map.of(QueryOperator.EQ.getValue(), SchemaStatus.PUBLISHED.getCode())));
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
