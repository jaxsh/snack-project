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

import org.jax.snack.framework.core.enums.BaseEnum;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.mybatisplus.converter.BasePageConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.upms.api.vo.SysNotificationVO;
import org.jax.snack.upms.biz.entity.SysNotification;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * 站内信转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface SysNotificationConverter extends BasePageConvert<SysNotification, SysNotificationVO> {

	@AfterMapping
	default void afterToVO(SysNotification entity, @MappingTarget SysNotificationVO vo) {
		if (entity.getReadFlag() != null) {
			vo.setReadFlagLabel(BaseEnum.getNameByCode(YesNoStatus.class, entity.getReadFlag()));
		}
	}

}
