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

import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;
import org.jax.snack.lowcode.biz.service.crud.DynamicValidator;

import org.springframework.stereotype.Component;

/**
 * 校验组件.
 * <p>
 * 对业务数据进行校验.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("validateCmp")
@RequiredArgsConstructor
public class ValidateComponent extends NodeComponent {

	private final DynamicValidator validator;

	@Override
	public void process() {
		BusinessFlowContext context = getContextBean(BusinessFlowContext.class);
		String schemaName = resolveSchemaName(context);

		log.debug("Validating data for schema: {}", schemaName);
		this.validator.validate(schemaName, context.getData());
		log.debug("Validation passed for schema: {}", schemaName);
	}

	private String resolveSchemaName(BusinessFlowContext context) {
		String tag = getTag();
		if (tag != null && !tag.isEmpty()) {
			return tag;
		}
		return context.getMainSchema();
	}

}
