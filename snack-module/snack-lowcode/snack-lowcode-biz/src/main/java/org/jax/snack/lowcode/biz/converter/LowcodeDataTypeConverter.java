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

import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.lowcode.api.vo.LowcodeDataTypeVO;
import org.jax.snack.lowcode.biz.entity.LowcodeDataType;
import org.mapstruct.Mapper;

/**
 * DataType 对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
@FunctionalInterface
public interface LowcodeDataTypeConverter {

	/**
	 * Entity 转 VO.
	 * @param entity 实体
	 * @return VO
	 */
	LowcodeDataTypeVO toVo(LowcodeDataType entity);

}
