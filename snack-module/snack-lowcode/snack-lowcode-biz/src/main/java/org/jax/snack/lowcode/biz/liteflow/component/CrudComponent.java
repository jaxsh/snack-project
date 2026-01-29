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

package org.jax.snack.lowcode.biz.liteflow.component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.api.service.DynamicCrudService;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * CRUD 操作组件.
 * <p>
 * 支持通过 Tag 指定操作类型和目标 Schema. Tag 格式: "operation:targetSchema", e.g.
 * "insert:order_history"
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("crudCmp")
@RequiredArgsConstructor
public class CrudComponent extends NodeComponent {

	private final DynamicCrudService dynamicCrudService;

	@Override
	public void process() {
		BusinessFlowContext context = getContextBean(BusinessFlowContext.class);
		TagInfo tagInfo = parseTag(context);

		log.debug("Executing CRUD operation: {} on schema: {}", tagInfo.operation, tagInfo.schemaName);

		switch (tagInfo.operation) {
			case INSERT -> doInsert(context, tagInfo.schemaName);
			case UPDATE -> doUpdate(context, tagInfo.schemaName);
			case DELETE -> doDelete(context, tagInfo.schemaName);
			case QUERY -> doQuery(context, tagInfo.schemaName);
		}
	}

	private void doInsert(BusinessFlowContext context, String schemaName) {
		Map<String, Object> data = resolveData(context);
		this.dynamicCrudService.create(schemaName, data);
		log.info("Inserted data into schema: {}", schemaName);
	}

	private void doUpdate(BusinessFlowContext context, String schemaName) {
		Map<String, Object> data = resolveData(context);
		Long id = resolveId(context, data);
		this.dynamicCrudService.update(schemaName, id, data);
		log.info("Updated data in schema: {}, id: {}", schemaName, id);
	}

	private void doDelete(BusinessFlowContext context, String schemaName) {
		Long id = context.getId();
		if (id != null) {
			this.dynamicCrudService.delete(schemaName, List.of(id));
			log.info("Deleted data from schema: {}, id: {}", schemaName, id);
		}
	}

	private void doQuery(BusinessFlowContext context, String schemaName) {
		Long id = context.getId();
		if (id != null) {
			this.dynamicCrudService.getById(schemaName, id).ifPresent(context::setResult);
		}
	}

	private Map<String, Object> resolveData(BusinessFlowContext context) {
		Object currentItem = context.getCurrentItem();
		if (currentItem instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> itemData = (Map<String, Object>) currentItem;
			return itemData;
		}
		return context.getData();
	}

	private Long resolveId(BusinessFlowContext context, Map<String, Object> data) {
		if (context.getId() != null) {
			return context.getId();
		}
		Object idValue = data.get("id");
		if (idValue instanceof Number n) {
			return n.longValue();
		}
		return null;
	}

	private TagInfo parseTag(BusinessFlowContext context) {
		String tag = getTag();
		CrudOperation operation = CrudOperation.INSERT;
		String schemaName = context.getMainSchema();

		if (StringUtils.hasText(tag)) {
			String[] parts = tag.split(":", 2);
			operation = CrudOperation.fromString(parts[0]);
			if (parts.length > 1 && StringUtils.hasText(parts[1])) {
				schemaName = parts[1];
			}
		}

		return new TagInfo(operation, schemaName);
	}

	private record TagInfo(CrudOperation operation, String schemaName) {
	}

	/**
	 * CRUD 操作类型.
	 */
	private enum CrudOperation {

		INSERT, UPDATE, DELETE, QUERY;

		static CrudOperation fromString(String value) {
			return switch (value.toLowerCase(Locale.ROOT)) {
				case "insert", "create" -> INSERT;
				case "update" -> UPDATE;
				case "delete" -> DELETE;
				case "query", "select" -> QUERY;
				default -> INSERT;
			};
		}

	}

}
