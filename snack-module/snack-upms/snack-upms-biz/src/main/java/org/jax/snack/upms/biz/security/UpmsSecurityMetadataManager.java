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

package org.jax.snack.upms.biz.security;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.upms.biz.entity.SysResource;
import org.jax.snack.upms.biz.repository.SysResourceRepository;

import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * 权限元数据管理器.
 * <p>
 * 负责加载和维护 URL 与权限标识的映射关系.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpmsSecurityMetadataManager {

	private final SysResourceRepository sysResourceRepository;

	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	@Getter
	private volatile Map<RequestMatcher, String> permissionRules = Map.of();

	/**
	 * 初始化权限规则.
	 */
	@PostConstruct
	public void init() {
		refresh();
	}

	/**
	 * 刷新权限规则.
	 */
	public void refresh() {
		QueryCondition condition = QueryCondition.builder()
			.eq(SysResource.Fields.type, 2)
			.eq(SysResource.Fields.status, 1)
			.build();

		List<SysResource> resources = this.sysResourceRepository.queryListByDsl(condition);

		resources.sort((r1, r2) -> {
			int order1 = (r1.getSortOrder() != null) ? r1.getSortOrder() : 0;
			int order2 = (r2.getSortOrder() != null) ? r2.getSortOrder() : 0;
			if (order1 != order2) {
				return Integer.compare(order1, order2);
			}
			String path1 = (r1.getPath() != null) ? r1.getPath() : "";
			String path2 = (r2.getPath() != null) ? r2.getPath() : "";
			return Integer.compare(path2.length(), path1.length());
		});

		Map<RequestMatcher, String> newRules = new LinkedHashMap<>();

		for (SysResource resource : resources) {
			if (StringUtils.hasText(resource.getPath()) && StringUtils.hasText(resource.getPermission())) {
				String path = resource.getPath();
				String method = resource.getMethod();
				String permission = resource.getPermission();

				RequestMatcher matcher = (request) -> {
					if (StringUtils.hasText(method) && !method.equalsIgnoreCase(request.getMethod())) {
						return false;
					}
					return this.pathMatcher.match(path, request.getServletPath());
				};

				newRules.put(matcher, permission);
			}
		}

		this.permissionRules = Collections.unmodifiableMap(newRules);
		log.info("Initialized {} dynamic permission rules from database.", this.permissionRules.size());
	}

}
