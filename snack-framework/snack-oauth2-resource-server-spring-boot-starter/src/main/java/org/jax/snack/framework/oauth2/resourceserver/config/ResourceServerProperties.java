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

package org.jax.snack.framework.oauth2.resourceserver.config;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Resource Server 配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snack.oauth2.resource-server")
public class ResourceServerProperties {

	/**
	 * 是否启用 Resource Server 自动配置.
	 */
	private boolean enabled = true;

	/**
	 * 是否验证 JWT 的 Audience (aud) claim.
	 * <p>
	 * 启用后, 将验证 token 的 aud 是否包含 spring.application.name.
	 */
	private boolean validateAudience = true;

	/**
	 * 允许匿名访问的路径.
	 */
	private List<String> permitAllPaths = List.of("/error", "/actuator/health");

}
