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
import org.jax.snack.framework.mybatisplus.converter.BasePageConvert;
import org.jax.snack.framework.utils.mapstruct.BaseDtoConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.enums.ResetCycle;
import org.jax.snack.upms.api.enums.SegmentType;
import org.jax.snack.upms.api.vo.SysIdRuleSegmentVO;
import org.jax.snack.upms.api.vo.SysIdRuleVO;
import org.jax.snack.upms.biz.entity.SysIdRule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * ID 规则对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface SysIdRuleConverter
		extends BaseDtoConvert<SysIdRuleDTO, SysIdRule, SysIdRuleVO>, BasePageConvert<SysIdRule, SysIdRuleVO> {

	@AfterMapping
	default void afterToVO(SysIdRule entity, @MappingTarget SysIdRuleVO vo) {
		if (StringUtils.hasText(entity.getResetCycle())) {
			vo.setResetCycleLabel(BaseEnum.getNameByCode(ResetCycle.class, entity.getResetCycle()));
		}
		if (!CollectionUtils.isEmpty(vo.getSegments())) {
			for (SysIdRuleSegmentVO segment : vo.getSegments()) {
				if (StringUtils.hasText(segment.getSegmentType())) {
					segment.setSegmentTypeLabel(BaseEnum.getNameByCode(SegmentType.class, segment.getSegmentType()));
				}
			}
		}
	}

}
