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

package org.jax.snack.upms.biz.repository;

import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.upms.biz.entity.SysOrgLevelName;

/**
 * 组织层级名称仓储接口.
 *
 * @author Jax Jiang
 */
public interface SysOrgLevelNameRepository {

	/**
	 * 保存层级名称配置.
	 * @param entity 层级名称实体
	 */
	void save(SysOrgLevelName entity);

	/**
	 * 根据 ID 查询.
	 * @param id 主键 ID
	 * @return Optional 包装的实体
	 */
	Optional<SysOrgLevelName> findById(Long id);

	/**
	 * 更新层级名称配置.
	 * @param entity 层级名称实体
	 */
	void update(SysOrgLevelName entity);

	/**
	 * 根据 ID 删除.
	 * @param id 主键 ID
	 */
	void deleteById(Long id);

	/**
	 * DSL 分页查询.
	 * @param condition 查询条件
	 * @return MyBatis-Plus 分页对象
	 */
	Page<SysOrgLevelName> queryPageByDsl(QueryCondition condition);

	/**
	 * DSL 列表查询.
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	List<SysOrgLevelName> queryListByDsl(QueryCondition condition);

}
