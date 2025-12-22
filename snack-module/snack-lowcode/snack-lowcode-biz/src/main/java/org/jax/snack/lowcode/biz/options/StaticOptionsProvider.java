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

import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

/**
 * 静态选项提供者.
 * <p>
 * 从 x-options.items 读取静态配置的选项列表.
 * </p>
 *
 * @author Jax Jiang
 */
@Component
public class StaticOptionsProvider implements OptionsProvider {

	private static final String TYPE = "static";

	@Override
	public List<OptionItem> getOptions(JsonNode xOptions) {
		JsonNode items = xOptions.path("items");
		if (items.isMissingNode() || !items.isArray()) {
			return List.of();
		}

		List<OptionItem> result = new ArrayList<>();
		for (JsonNode item : items) {
			String label = item.path("label").asString("");
			JsonNode valueNode = item.path("value");
			Object value = extractValue(valueNode);
			result.add(new OptionItem(label, value));
		}
		return result;
	}

	@Override
	public boolean supports(String type) {
		return TYPE.equals(type);
	}

	@Override
	public int getOrder() {
		return 10;
	}

	private Object extractValue(JsonNode node) {
		if (node.isInt()) {
			return node.asInt();
		}
		if (node.isLong()) {
			return node.asLong();
		}
		if (node.isDouble()) {
			return node.asDouble();
		}
		if (node.isBoolean()) {
			return node.asBoolean();
		}
		return node.asString();
	}

}
