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

package org.jax.snack.framework.mybatisplus.query;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

/**
 * JsonNullable 工具类.
 * <p>
 * 用于从 DTO 中识别需要显式置为 NULL 的清空字段, 供 DSL 更新构造 setData.
 *
 * @author Jax Jiang
 */
public final class JsonNullableSupport {

	private JsonNullableSupport() {
	}

	/**
	 * 收集 DTO 中"present 且值为 null"的 JsonNullable 字段.
	 * <p>
	 * 字段值经 BeanWrapper 的属性 getter 读取. "未传"(undefined)与"传值"的字段不会被收集.
	 * @param dto DTO 对象
	 * @return 清空字段映射 (key=字段名, value=null)
	 */
	public static Map<String, Object> clearedFields(Object dto) {
		Map<String, Object> map = new HashMap<>();
		BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(dto);
		for (PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
			String name = descriptor.getName();
			if (beanWrapper.isReadableProperty(name)
					&& beanWrapper.getPropertyValue(name) instanceof JsonNullable<?> nullable && nullable.isPresent()
					&& nullable.get() == null) {
				map.put(name, null);
			}
		}
		return map;
	}

}
