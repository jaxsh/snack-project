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

package org.jax.snack.lowcode.biz.converter;

import org.jax.snack.framework.core.enums.BaseEnum;
import org.jax.snack.framework.mybatisplus.converter.BasePageConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.enums.SchemaStatus;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Schema 对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface LowcodeSchemaConverter extends BasePageConvert<LowcodeSchema, LowcodeSchemaVO> {

	/**
	 * DTO 转 Entity.
	 * @param dto DTO
	 * @return Entity
	 */
	LowcodeSchema toEntity(LowcodeSchemaDTO dto);

	/**
	 * Entity 转 VO.
	 * @param entity 实体
	 * @return VO
	 */
	LowcodeSchemaVO toVo(LowcodeSchema entity);

	/**
	 * 复制草稿为发布版本.
	 * <p>
	 * 忽略 id 字段，status 需要在调用后手动设置.
	 * @param draft 草稿实体
	 * @return 用于发布的新实体
	 */
	@Mapping(target = "id", ignore = true)
	LowcodeSchema copyForPublish(LowcodeSchema draft);

	/**
	 * 从草稿更新已发布版本.
	 * <p>
	 * 同步所有字段，忽略 id 和 status.
	 * @param draft 草稿实体 (源)
	 * @param published 已发布实体 (目标)
	 */
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "status", ignore = true)
	void updateFromDraft(LowcodeSchema draft, @MappingTarget LowcodeSchema published);

	@AfterMapping
	default void afterToVo(LowcodeSchema entity, @MappingTarget LowcodeSchemaVO vo) {
		if (entity.getStatus() != null) {
			vo.setStatusLabel(BaseEnum.getNameByCode(SchemaStatus.class, entity.getStatus()));
		}
	}

}
