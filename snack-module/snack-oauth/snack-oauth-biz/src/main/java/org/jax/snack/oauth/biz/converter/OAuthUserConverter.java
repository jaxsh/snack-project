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

package org.jax.snack.oauth.biz.converter;

import org.jax.snack.framework.core.enums.BaseEnum;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.utils.mapstruct.BaseDtoConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.oauth.api.vo.OAuthUserVO;
import org.jax.snack.oauth.biz.entity.OAuthUser;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * OAuth2 用户转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface OAuthUserConverter extends BaseDtoConvert<OAuthUserDTO, OAuthUser, OAuthUserVO> {

	@AfterMapping
	default void afterToVO(OAuthUser entity, @MappingTarget OAuthUserVO vo) {
		if (entity.getEnabled() != null) {
			vo.setEnabledLabel(BaseEnum.getNameByCode(Status.class, entity.getEnabled()));
		}
		if (entity.getLocked() != null) {
			vo.setLockedLabel(BaseEnum.getNameByCode(YesNoStatus.class, entity.getLocked()));
		}
		if (entity.getExpired() != null) {
			vo.setExpiredLabel(BaseEnum.getNameByCode(YesNoStatus.class, entity.getExpired()));
		}
		if (entity.getInitialPassword() != null) {
			vo.setInitialPasswordLabel(BaseEnum.getNameByCode(YesNoStatus.class, entity.getInitialPassword()));
		}
	}

}
