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
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.lowcode.biz.entity.LowcodeDataType;
import org.jax.snack.lowcode.biz.repository.LowcodeDataTypeRepository;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 数据类型管理器.
 * <p>
 * 负责解析和映射数据类型，支持从数据库配置中加载类型定义.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataTypeManager {

	private final LowcodeDataTypeRepository dataTypeRepository;

	/**
	 * 获取类型配置.
	 * @param logicType 逻辑类型
	 * @return 配置对象 (可能为 null)
	 */
	public LowcodeDataType getConfig(String logicType) {
		if (!StringUtils.hasText(logicType)) {
			return null;
		}
		List<LowcodeDataType> dataTypes = findAllEnabledDataTypes();
		Map<String, LowcodeDataType> typeMap = dataTypes.stream()
			.collect(Collectors.toMap((dt) -> dt.getLogicType().toUpperCase(Locale.ROOT), Function.identity(),
					(v1, v2) -> v1));

		return typeMap.get(logicType.toUpperCase(Locale.ROOT));
	}

	/**
	 * 构建 SQL 类型字符串.
	 * @param type 逻辑类型 (e.g., "string", "long_text")
	 * @param length 指定长度
	 * @param scale 指定精度
	 * @return 标准 SQL 类型字符串 (e.g., "VARCHAR(255)")
	 */
	public String resolveSqlType(String type, int length, int scale) {
		String normalizedLogicType = type.toUpperCase(Locale.ROOT);

		LowcodeDataType config = getConfig(normalizedLogicType);

		if (config == null) {
			return buildDefault(normalizedLogicType, length, scale);
		}

		String dbType = config.getDbType().toUpperCase(Locale.ROOT);
		int finalLength = (length > 0) ? length
				: ((!ObjectUtils.isEmpty(config.getDefaultLength())) ? config.getDefaultLength() : 0);
		int finalScale = (scale > 0) ? scale
				: ((!ObjectUtils.isEmpty(config.getDefaultScale())) ? config.getDefaultScale() : 0);

		boolean needLength = Boolean.TRUE.equals(config.getNeedLength());
		boolean needScale = Boolean.TRUE.equals(config.getNeedScale());

		if (needLength) {
			if (needScale) {
				return dbType + "(" + finalLength + "," + finalScale + ")";
			}
			return dbType + "(" + finalLength + ")";
		}

		return dbType;
	}

	private String buildDefault(String type, int length, int scale) {
		if (length > 0) {
			if (scale > 0) {
				return type + "(" + length + "," + scale + ")";
			}
			return type + "(" + length + ")";
		}
		return type;
	}

	/**
	 * 根据逻辑类型获取完整的类型配置.
	 * @param logicType 逻辑类型 (e.g., "string", "int")
	 * @return 类型配置, 如果不存在返回 null
	 */
	public LowcodeDataType getDataType(String logicType) {
		if (!StringUtils.hasText(logicType)) {
			return null;
		}
		List<LowcodeDataType> dataTypes = findAllEnabledDataTypes();
		return dataTypes.stream()
			.filter((dt) -> logicType.equalsIgnoreCase(dt.getLogicType()))
			.findFirst()
			.orElse(null);
	}

	private List<LowcodeDataType> findAllEnabledDataTypes() {
		QueryCondition condition = QueryCondition.builder().eq("enabled", true).orderByAsc("sortOrder").build();
		return this.dataTypeRepository.queryListByDsl(condition);
	}

}
