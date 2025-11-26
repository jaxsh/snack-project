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

package org.jax.snack.framework.web.config;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.http.client.autoconfigure.HttpClientsProperties;
import org.springframework.boot.jackson.autoconfigure.JacksonProperties;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 配置属性有效性验证测试. 验证属性存在性及类型匹配.
 *
 * @author Jax Jiang
 */
class ConfigurationPropertiesValidationTests {

	static Stream<Arguments> configurationKeysProvider() {
		return Stream.of(
				// Jackson
				Arguments.of(JacksonProperties.class, "spring.jackson.default-property-inclusion",
						JsonInclude.Include.class),
				Arguments.of(JacksonProperties.class, "spring.jackson.deserialization.fail-on-unknown-properties",
						Boolean.class),
				Arguments.of(JacksonProperties.class, "spring.jackson.serialization.fail-on-empty-beans",
						Boolean.class),
				// MessageSource
				Arguments.of(MessageSourceProperties.class, "spring.messages.basename", List.class),
				Arguments.of(MessageSourceProperties.class, "spring.messages.use-code-as-default-message",
						Boolean.class),
				// Http Clients
				Arguments.of(HttpClientsProperties.class, "spring.http.clients.connect-timeout", Duration.class),
				Arguments.of(HttpClientsProperties.class, "spring.http.clients.read-timeout", Duration.class));
	}

	@ParameterizedTest(name = "Validation for key: {1}")
	@MethodSource("configurationKeysProvider")
	void shouldValidateConfigurationKeys(Class<?> propertiesClass, String configKey, Class<?> expectedType) {
		verifyConfigurationKeyExists(propertiesClass, configKey, expectedType);
	}

	private void verifyConfigurationKeyExists(Class<?> propertiesClass, String configKey, Class<?> expectedValueType) {
		ConfigurationProperties annotation = propertiesClass.getAnnotation(ConfigurationProperties.class);
		assertThat(annotation).isNotNull();

		String expectedPrefix = annotation.prefix();
		if (!StringUtils.hasText(expectedPrefix)) {
			expectedPrefix = annotation.value();
		}
		assertThat(expectedPrefix).isNotEmpty();

		assertThat(configKey).startsWith(expectedPrefix);

		String propertyPath = configKey.substring(expectedPrefix.length() + 1);
		String[] pathSegments = propertyPath.split("\\.");

		ResolvableType currentType = ResolvableType.forClass(propertiesClass);

		for (int i = 0; i < pathSegments.length; i++) {
			String segment = pathSegments[i];
			boolean isLastSegment = (i == pathSegments.length - 1);
			Class<?> resolvedClass = currentType.resolve(Object.class);

			if (Map.class.isAssignableFrom(resolvedClass)) {
				currentType = handleMapSegment(currentType, segment, expectedValueType, isLastSegment);
			}
			else {
				currentType = handlePojoSegment(resolvedClass, segment, expectedValueType, isLastSegment);
			}
		}
	}

	private ResolvableType handleMapSegment(ResolvableType currentType, String segment, Class<?> expectedValueType,
			boolean isLastSegment) {
		ResolvableType keyType = currentType.getGeneric(0);
		validateMapKey(keyType, segment);

		ResolvableType valueType = currentType.getGeneric(1);
		if (isLastSegment) {
			assertTypeMatches(valueType.resolve(Object.class), expectedValueType);
		}
		return valueType;
	}

	private ResolvableType handlePojoSegment(Class<?> resolvedClass, String segment, Class<?> expectedValueType,
			boolean isLastSegment) {
		String propertyName = kebabToCamelCase(segment);
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(resolvedClass, propertyName);

		assertThat(pd).isNotNull();

		if (isLastSegment) {
			assertTypeMatches(pd.getPropertyType(), expectedValueType);
			return ResolvableType.NONE;
		}

		Method readMethod = pd.getReadMethod();
		assertThat(readMethod).isNotNull();

		return ResolvableType.forMethodReturnType(readMethod);
	}

	private void assertTypeMatches(Class<?> actualType, Class<?> expectedType) {
		assertThat(actualType).isNotNull();
		Class<?> boxedActual = ClassUtils.resolvePrimitiveIfNecessary(actualType);
		Class<?> boxedExpected = ClassUtils.resolvePrimitiveIfNecessary(expectedType);

		assertThat(boxedExpected.isAssignableFrom(boxedActual)).isTrue();
	}

	private void validateMapKey(ResolvableType keyType, String segment) {
		Class<?> keyClass = keyType.resolve();
		if (keyClass != null && keyClass.isEnum()) {
			String enumConstantName = segment.replace('-', '_').toUpperCase(Locale.ROOT);
			boolean found = false;
			for (Object constant : keyClass.getEnumConstants()) {
				if (constant.toString().equals(enumConstantName)) {
					found = true;
					break;
				}
			}
			assertThat(found).isTrue();
		}
	}

	private String kebabToCamelCase(String kebab) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = false;
		char hyphen = '-';

		for (char c : kebab.toCharArray()) {
			if (c == hyphen) {
				capitalizeNext = true;
			}
			else if (capitalizeNext) {
				result.append(Character.toUpperCase(c));
				capitalizeNext = false;
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}

}
