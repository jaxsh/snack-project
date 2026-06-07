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

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.jax.snack.upms.biz.converter.SysSessionConverter;
import org.jspecify.annotations.Nullable;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
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
	public List<SysSessionVO> getSessions(@Nullable String username) {
		return this.sessionRegistry.getAllPrincipals()
			.stream()
			.filter((p) -> username == null || username.equals(extractUsername(p)))
			.flatMap((p) -> this.sessionRegistry.getAllSessions(p, false).stream())
			.map(this.converter::toVO)
			.toList();
	}

	@Override
	public void revokeSession(String sessionId) {
		SessionInformation info = this.sessionRegistry.getSessionInformation(sessionId);
		if (info == null) {
			throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "Session");
		}
		info.expireNow();
	}

	private static String extractUsername(Object principal) {
		if (principal instanceof UserDetails u) {
			return u.getUsername();
		}
		if (principal instanceof OAuth2AuthenticatedPrincipal o) {
			return o.getName();
		}
		return String.valueOf(principal);
	}

}
