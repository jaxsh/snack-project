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

package org.jax.snack.upms.biz.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.jax.snack.upms.biz.converter.SysSessionConverter;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 活跃 Session 管理服务实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysSessionServiceImpl implements SysSessionService {

	private final SessionRegistry sessionRegistry;

	private final SysSessionConverter converter;

	@Override
	public List<SysSessionVO> getSessions(String username) {
		return sessionsOf(username).stream()
			.map(this.converter::toVO)
			.collect(Collectors.toMap(SysSessionVO::getSessionId, (vo) -> vo,
					(existing, replacement) -> existing.getLastRequest().isAfter(replacement.getLastRequest())
							? existing : replacement,
					LinkedHashMap::new))
			.values()
			.stream()
			.toList();
	}

	@Override
	public void revokeSession(String username, String sessionId) {
		if (!StringUtils.hasText(sessionId)) {
			sessionsOf(username).forEach(SessionInformation::expireNow);
		}
		else {
			sessionsOf(username).stream()
				.filter((info) -> sessionId.equals(info.getSessionId()))
				.findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Session"))
				.expireNow();
		}
	}

	private List<SessionInformation> sessionsOf(String username) {
		return this.sessionRegistry.getAllPrincipals()
			.stream()
			.flatMap((p) -> this.sessionRegistry.getAllSessions(p, false).stream())
			.filter((info) -> username.equals(this.converter.toVO(info).getUsername()))
			.toList();
	}

}
