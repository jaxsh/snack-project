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

package org.jax.snack.lowcode.biz.service.crud;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import org.jax.snack.lowcode.biz.validation.SimpleConstraintViolation;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * 动态数据校验器.
 * <p>
 * 基于 JSON Schema Draft-07 标准进行数据校验, 直接使用存储的 schema_json 校验.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicValidator {

	private static final String KEYWORD_REQUIRED = "required";

	private final SchemaService schemaService;

	private final JsonMapper jsonMapper;

	private final SchemaRegistry schemaRegistry = SchemaRegistry.withDialect(Dialects.getDraft7());

	/**
	 * 校验数据.
	 * <p>
	 * 直接使用存储的 JSON Schema 进行校验, 不做任何转换.
	 * </p>
	 * @param schemaName Schema 名称
	 * @param data 待校验数据
	 * @throws ConstraintViolationException 校验失败时抛出
	 */
	public void validate(String schemaName, Map<String, Object> data) {
		// 1. 获取存储的 JSON Schema
		JsonNode schemaNode = this.schemaService.getSchema(schemaName);

		// 2. 转换数据为 JsonNode
		JsonNode dataNode = this.jsonMapper.valueToTree(data);

		// 3. 创建校验器并执行校验
		Schema schema = this.schemaRegistry.getSchema(schemaNode);
		Locale locale = LocaleContextHolder.getLocale();
		List<Error> errors = schema.validate(dataNode,
				(ctx) -> ctx.executionConfig((config) -> config.locale(locale).failFast(false)));

		// 4. 如果有错误，转换为 ConstraintViolationException
		if (!errors.isEmpty()) {
			Set<ConstraintViolation<?>> violations = new HashSet<>();
			for (Error error : errors) {
				String path = extractFieldName(error);
				String message = error.getMessage();
				violations.add(SimpleConstraintViolation.of(message, path, null));
			}
			log.warn("数据校验失败 [{}]: {}", schemaName, violations);
			throw new ConstraintViolationException(violations);
		}
	}

	/**
	 * 从 Error 中提取字段名.
	 * @param error 校验错误
	 * @return 字段名
	 */
	private String extractFieldName(Error error) {
		String errorType = error.getKeyword();
		if (KEYWORD_REQUIRED.equals(errorType)) {
			Object[] args = error.getArguments();
			if (args != null && args.length > 0 && args[0] instanceof String) {
				return (String) args[0];
			}
		}
		if (error.getInstanceLocation() != null) {
			String path = error.getInstanceLocation().toString();
			if (path.startsWith("/")) {
				int lastSlash = path.lastIndexOf('/');
				return path.substring(lastSlash + 1);
			}
			return path;
		}
		return "unknown";
	}

}
