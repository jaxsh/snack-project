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

package org.jax.snack.lowcode.biz.repository;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.lowcode.biz.entity.LowcodeDataType;

/**
 * 数据类型仓储接口.
 *
 * @author Jax Jiang
 */
public interface LowcodeDataTypeRepository {

	/**
	 * DSL 存在性查询.
	 * @param condition 查询条件
	 * @return 是否存在
	 */
	boolean existsByDsl(QueryCondition condition);

	/**
	 * DSL 分页查询.
	 * @param condition 查询条件
	 * @return MyBatis-Plus 分页对象
	 */
	Page<LowcodeDataType> queryPageByDsl(QueryCondition condition);

	/**
	 * DSL 列表查询.
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	List<LowcodeDataType> queryListByDsl(QueryCondition condition);

}
