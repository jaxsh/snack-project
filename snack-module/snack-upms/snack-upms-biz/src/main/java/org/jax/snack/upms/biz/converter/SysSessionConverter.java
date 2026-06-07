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

package org.jax.snack.upms.biz.converter;

import java.time.ZoneId;

import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/**
 * 活跃 Session 对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface SysSessionConverter {

	/**
	 * SessionInformation → SysSessionVO.
	 * <p>
	 * sessionId 按名称自动映射，username 和 lastRequest 在 {@link #afterToVO} 中处理.
	 * @param info Session 信息
	 * @return SysSessionVO
	 */
	SysSessionVO toVO(SessionInformation info);

	/**
	 * 补充无法直接映射的字段.
	 * @param info Session 信息
	 * @param vo 目标 VO
	 */
	@AfterMapping
	default void afterToVO(SessionInformation info, @MappingTarget SysSessionVO vo) {
		Object principal = info.getPrincipal();
		if (principal instanceof UserDetails u) {
			vo.setUsername(u.getUsername());
		}
		else if (principal instanceof OAuth2AuthenticatedPrincipal o) {
			vo.setUsername(o.getName());
		}
		else {
			vo.setUsername(String.valueOf(principal));
		}
		if (info.getLastRequest() != null) {
			vo.setLastRequest(info.getLastRequest().toInstant().atZone(ZoneId.systemDefault()));
		}
	}

}
