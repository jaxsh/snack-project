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

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.jax.snack.upms.biz.converter.SysSessionConverter;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;

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
		return sessionsOf(username).stream().map(this.converter::toVO).toList();
	}

	@Override
	public void revokeSession(String username, String sessionId) {
		sessionsOf(username).stream()
			.filter((info) -> sessionId.equals(info.getSessionId()))
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND, "Session"))
			.expireNow();
	}

	@Override
	public void revokeSessionsByUsername(String username) {
		sessionsOf(username).forEach(SessionInformation::expireNow);
	}

	@Override
	public Map<String, ZonedDateTime> getLastActiveTimes() {
		Map<String, ZonedDateTime> result = new HashMap<>();
		this.sessionRegistry.getAllPrincipals()
			.stream()
			.flatMap((p) -> this.sessionRegistry.getAllSessions(p, false).stream())
			.map(this.converter::toVO)
			.forEach((vo) -> result.merge(vo.getUsername(), vo.getLastRequest(), (a, b) -> a.isAfter(b) ? a : b));
		return result;
	}

	private List<SessionInformation> sessionsOf(String username) {
		return this.sessionRegistry.getAllPrincipals()
			.stream()
			.flatMap((p) -> this.sessionRegistry.getAllSessions(p, false).stream())
			.filter((info) -> username.equals(this.converter.toVO(info).getUsername()))
			.toList();
	}

}
