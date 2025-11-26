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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.NullMarked;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 *
 * 环境后处理器. 用于在应用程序启动时处理环境变量和配置.
 *
 * @author Jax Jiang
 *
 */

public class WebConfigEnvironment implements EnvironmentPostProcessor {

	@NullMarked
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

		Map<String, Object> customerProperties = new HashMap<>();

		customerProperties.put("spring.messages.basename", "i18n/messages");
		customerProperties.put("spring.messages.use-code-as-default-message", true);

		customerProperties.put("spring.http.clients.connect-timeout", Duration.ofSeconds(5));
		customerProperties.put("spring.http.clients.read-timeout", Duration.ofSeconds(10));

		customerProperties.put("logbook.filter.enabled", false);

		customerProperties.put("spring.jackson.default-property-inclusion", JsonInclude.Include.NON_NULL.name());
		customerProperties.put("spring.jackson.deserialization.fail-on-unknown-properties", false);
		customerProperties.put("spring.jackson.serialization.fail-on-empty-beans", false);

		MapPropertySource mapPropertySource = new MapPropertySource("customerProperties", customerProperties);
		environment.getPropertySources().addLast(mapPropertySource);

	}

}
