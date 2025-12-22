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

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 选项解析器.
 * <p>
 * 统一调度不同类型的 OptionsProvider.
 * </p>
 *
 * @author Jax Jiang
 */
@Component
public class OptionsResolver {

	private final List<OptionsProvider> providers;

	public OptionsResolver(List<OptionsProvider> providers) {
		this.providers = providers.stream().sorted(Comparator.comparingInt(OptionsProvider::getOrder)).toList();
	}

	/**
	 * 获取选项列表.
	 * @param xOptions x-options 配置节点
	 * @return 选项列表
	 */
	public List<OptionItem> resolveOptions(JsonNode xOptions) {
		if (xOptions == null || xOptions.isMissingNode()) {
			return List.of();
		}

		String type = xOptions.path("type").asString();
		if (!StringUtils.hasText(type)) {
			return List.of();
		}

		return this.providers.stream()
			.filter((p) -> p.supports(type))
			.findFirst()
			.map((p) -> p.getOptions(xOptions))
			.orElse(List.of());
	}

	/**
	 * 值转标签.
	 * @param xOptions x-options 配置节点
	 * @param value 存储值
	 * @return 显示标签
	 */
	public String getLabel(JsonNode xOptions, Object value) {
		if (value == null) {
			return "";
		}

		String valueStr = String.valueOf(value);
		return resolveOptions(xOptions).stream()
			.filter((opt) -> Objects.equals(valueStr, opt.valueAsString()))
			.findFirst()
			.map(OptionItem::label)
			.orElse(valueStr);
	}

	/**
	 * 标签转值.
	 * @param xOptions x-options 配置节点
	 * @param label 显示标签
	 * @return 存储值
	 */
	public Object getValue(JsonNode xOptions, String label) {
		if (!StringUtils.hasText(label)) {
			return null;
		}

		return resolveOptions(xOptions).stream()
			.filter((opt) -> Objects.equals(label, opt.label()))
			.findFirst()
			.map(OptionItem::value)
			.orElse(label);
	}

}
