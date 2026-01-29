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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.yomahub.liteflow.core.NodeIteratorComponent;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;

import org.springframework.stereotype.Component;

/**
 * 迭代器组件.
 * <p>
 * 用于循环处理数组字段中的每一项.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("iteratorCmp")
public class IteratorComponent extends NodeIteratorComponent {

	private static final String CONFIG_DATA_FIELD = "dataField";

	@Override
	public Iterator<?> processIterator() {
		BusinessFlowContext context = getContextBean(BusinessFlowContext.class);
		String stepId = getNodeId();
		Map<String, Object> config = context.getStepConfig(stepId);

		String dataField = (String) config.getOrDefault(CONFIG_DATA_FIELD, "items");
		Object fieldValue = context.getData().get(dataField);

		if (fieldValue instanceof List<?> list) {
			log.debug("Iterating over field '{}' with {} items", dataField, list.size());
			return list.iterator();
		}

		log.warn("Field '{}' is not a list, returning empty iterator", dataField);
		return Collections.emptyIterator();
	}

}
