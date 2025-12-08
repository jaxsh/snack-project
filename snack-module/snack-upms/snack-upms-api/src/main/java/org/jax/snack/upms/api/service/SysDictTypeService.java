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

package org.jax.snack.upms.api.service;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysDictTypeDTO;
import org.jax.snack.upms.api.vo.SysDictTypeVO;

/**
 * 字典类型服务接口.
 *
 * @author Jax Jiang
 */
public interface SysDictTypeService {

	/**
	 * 创建字典类型.
	 * @param dto 字典类型 DTO.
	 */
	void create(SysDictTypeDTO dto);

	/**
	 * 更新字典类型.
	 * @param id 主键 ID.
	 * @param dto 字典类型 DTO.
	 */
	void update(Long id, SysDictTypeDTO dto);

	/**
	 * 删除字典类型.
	 * @param id 主键 ID.
	 */
	void deleteById(Long id);

	/**
	 * 使用 JSON DSL 查询字典类型.
	 * @param condition 查询条件
	 * @return 分页结果（无论是否分页，总是返回分页结构）
	 */
	PageResult<SysDictTypeVO> queryByDsl(QueryCondition condition);

}
