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

package org.jax.snack.framework.core.enums;

/**
 * 通用枚举接口.
 * <p>
 * 所有业务枚举都应实现此接口，提供统一的 code 和 name 访问方法。
 *
 * @param <T> 枚举值类型（通常为 Integer 或 String）
 * @author Jax Jiang
 */
public interface BaseEnum<T> {

	/**
	 * 获取枚举值（用于存储）.
	 * @return 枚举值
	 */
	T getCode();

	/**
	 * 获取枚举名称（用于显示）.
	 * @return 枚举名称
	 */
	String getName();

	/**
	 * 根据 code 获取枚举实例的工具方法.
	 * @param enumClass 枚举类
	 * @param code 枚举值
	 * @param <E> 枚举类型
	 * @param <T> code 类型
	 * @return 枚举实例，如果未找到则返回 null
	 */
	static <E extends Enum<E> & BaseEnum<T>, T> E fromCode(Class<E> enumClass, T code) {
		if (code == null) {
			return null;
		}
		for (E enumConstant : enumClass.getEnumConstants()) {
			if (enumConstant.getCode().equals(code)) {
				return enumConstant;
			}
		}
		return null;
	}

	/**
	 * 根据 code 获取枚举名称.
	 * @param enumClass 枚举类
	 * @param code 枚举值
	 * @param <E> 枚举类型
	 * @param <T> code 类型
	 * @return 枚举名称，如果未找到则返回 null
	 */
	static <E extends Enum<E> & BaseEnum<T>, T> String getNameByCode(Class<E> enumClass, T code) {
		E enumInstance = fromCode(enumClass, code);
		return (enumInstance != null) ? enumInstance.getName() : null;
	}

}
