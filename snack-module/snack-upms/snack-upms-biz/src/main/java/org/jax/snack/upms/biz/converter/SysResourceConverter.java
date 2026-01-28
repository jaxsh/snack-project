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
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.mybatisplus.converter.BasePageConvert;
import org.jax.snack.framework.utils.mapstruct.BaseDtoConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.upms.api.dto.SysResourceDTO;
import org.jax.snack.upms.api.enums.HttpMethod;
import org.jax.snack.upms.api.enums.ResourceType;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.biz.entity.SysResource;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

/**
 * 资源对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface SysResourceConverter extends BaseDtoConvert<SysResourceDTO, SysResource, SysResourceVO>,
		BasePageConvert<SysResource, SysResourceVO> {

	@AfterMapping
	default void afterToVO(SysResource entity, @MappingTarget SysResourceVO vo) {
		if (entity.getType() != null) {
			vo.setTypeLabel(BaseEnum.getNameByCode(ResourceType.class, entity.getType()));
		}
		if (entity.getMethod() != null) {
			vo.setMethodLabel(BaseEnum.getNameByCode(HttpMethod.class, entity.getMethod()));
		}
		if (entity.getVisible() != null) {
			vo.setVisibleLabel(BaseEnum.getNameByCode(YesNoStatus.class, entity.getVisible()));
		}
		if (entity.getStatus() != null) {
			vo.setStatusLabel(BaseEnum.getNameByCode(Status.class, entity.getStatus()));
		}
	}

}
