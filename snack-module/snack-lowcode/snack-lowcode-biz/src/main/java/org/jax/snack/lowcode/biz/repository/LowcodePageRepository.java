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
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.lowcode.biz.entity.LowcodePage;

/**
 * 页面配置仓储接口.
 *
 * @author Jax Jiang
 */
public interface LowcodePageRepository {

	/**
	 * 保存页面.
	 * @param entity 页面实体
	 */
	void save(LowcodePage entity);

	/**
	 * 更新页面.
	 * @param entity 页面实体
	 */
	void update(LowcodePage entity);

	/**
	 * 根据 ID 查询.
	 * @param id 主键 ID
	 * @return Optional 包装的实体
	 */
	Optional<LowcodePage> findById(Long id);

	/**
	 * 根据 ID 删除.
	 * @param id 主键 ID
	 */
	void deleteById(Long id);

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
	Page<LowcodePage> queryPageByDsl(QueryCondition condition);

	/**
	 * DSL 列表查询.
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	List<LowcodePage> queryListByDsl(QueryCondition condition);

}
