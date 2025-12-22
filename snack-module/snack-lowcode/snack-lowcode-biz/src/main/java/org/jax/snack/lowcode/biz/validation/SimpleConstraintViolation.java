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

package org.jax.snack.lowcode.biz.validation;

import java.util.Collections;
import java.util.Iterator;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * 简单的 ConstraintViolation 实现.
 * <p>
 * 用于动态校验时构建错误信息，适配 jakarta.validation 异常体系.
 *
 * @param <T> Bean 类型
 * @author Jax Jiang
 */
@Getter
@Builder
public class SimpleConstraintViolation<T> implements ConstraintViolation<T> {

	private final String message;

	private final String messageTemplate;

	private final T rootBean;

	private final Class<T> rootBeanClass;

	private final Object leafBean;

	private final Object[] executableParameters;

	private final Object executableReturnValue;

	private final Path propertyPath;

	private final Object invalidValue;

	private final ConstraintDescriptor<?> constraintDescriptor;

	@Override
	public <U> U unwrap(Class<U> type) {
		return null;
	}

	public static <T> SimpleConstraintViolation<T> of(String message, String property, Object invalidValue) {
		return SimpleConstraintViolation.<T>builder()
			.message(message)
			.invalidValue(invalidValue)
			.propertyPath(new SimplePath(property))
			.build();
	}

	// 简易 Path 实现
	private static class SimplePath implements Path {

		private final String property;

		SimplePath(String property) {
			this.property = property;
		}

		@NonNull
		@Override
		public Iterator<Node> iterator() {
			return Collections.singletonList((Node) new SimpleNode(this.property)).iterator();
		}

		@Override
		public String toString() {
			return this.property;
		}

	}

	// 简易 Node 实现
	private static class SimpleNode implements Path.Node {

		private final String name;

		SimpleNode(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isInIterable() {
			return false;
		}

		@Override
		public Integer getIndex() {
			return null;
		}

		@Override
		public Object getKey() {
			return null;
		}

		@Override
		public ElementKind getKind() {
			return ElementKind.PROPERTY;
		}

		@Override
		public <T extends Path.Node> T as(Class<T> nodeType) {
			return null;
		}

	}

}
