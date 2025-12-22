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

package org.jax.snack.lowcode.biz.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.enums.BaseEnum;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

/**
 * 枚举选项提供者.
 * <p>
 * 从 Java 枚举类获取选项列表.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
public class EnumOptionsProvider implements OptionsProvider {

	private static final String TYPE = "enum";

	private final Map<String, List<OptionItem>> cache = new ConcurrentHashMap<>();

	@Override
	public List<OptionItem> getOptions(JsonNode xOptions) {
		String enumClass = xOptions.path("enumClass").asString();
		if (enumClass.isEmpty()) {
			log.warn("enumClass is empty in x-options");
			return List.of();
		}

		return this.cache.computeIfAbsent(enumClass, this::loadEnumOptions);
	}

	@Override
	public boolean supports(String type) {
		return TYPE.equals(type);
	}

	@Override
	public int getOrder() {
		return 30;
	}

	private List<OptionItem> loadEnumOptions(String enumClassName) {
		try {
			Class<?> enumClass = Class.forName(enumClassName);
			if (!enumClass.isEnum()) {
				log.warn("Class {} is not an enum", enumClassName);
				return List.of();
			}

			List<OptionItem> result = new ArrayList<>();
			for (Object constant : enumClass.getEnumConstants()) {
				if (constant instanceof BaseEnum<?> baseEnum) {
					result.add(new OptionItem(baseEnum.getName(), baseEnum.getCode()));
				}
				else {
					// 普通枚举
					Enum<?> e = (Enum<?>) constant;
					result.add(new OptionItem(e.name(), e.ordinal()));
				}
			}
			return result;
		}
		catch (ClassNotFoundException ex) {
			log.error("Enum class not found: {}", enumClassName, ex);
			return List.of();
		}
	}

}
