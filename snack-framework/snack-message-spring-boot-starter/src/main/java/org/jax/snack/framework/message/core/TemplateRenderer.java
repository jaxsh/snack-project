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

package org.jax.snack.framework.message.core;

import java.util.Map;

import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.StringUtils;

/**
 * 模版渲染器, 支持 ${xxx} 占位符语法.
 *
 * @author Jax Jiang
 */
public class TemplateRenderer {

	private final PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}");

	/**
	 * 渲染模版, 替换占位符.
	 * @param template 模版内容
	 * @param params 占位符参数
	 * @return 渲染后的内容
	 */
	public String render(String template, Map<String, Object> params) {
		if (!StringUtils.hasText(template)) {
			return template;
		}
		return this.helper.replacePlaceholders(template, (placeholderName) -> {
			String key = placeholderName;
			String defaultValue = null;
			int separatorIndex = placeholderName.indexOf(':');
			if (separatorIndex != -1) {
				key = placeholderName.substring(0, separatorIndex);
				defaultValue = placeholderName.substring(separatorIndex + 1);
			}
			Object value = params.get(key);
			if (value != null) {
				return String.valueOf(value);
			}
			return defaultValue;
		});
	}

}
