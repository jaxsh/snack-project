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

package org.jax.snack.framework.mybatisplus.repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.UpdateCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;

/**
 * 通用 Repository 基础接口.
 * <p>
 * 定义标准 CRUD 操作契约, 所有 Repository 接口应参考此规范.
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author Jax Jiang
 */
public interface BaseRepository<T, ID extends Serializable> {

	/**
	 * 保存实体.
	 * @param entity 实体对象
	 */
	void save(T entity);

	/**
	 * 批量保存实体.
	 * @param entities 实体集合
	 */
	void saveBatch(Collection<T> entities);

	/**
	 * 根据 ID 查询.
	 * @param id 主键 ID
	 * @return Optional 包装的实体
	 */
	Optional<T> findById(ID id);

	/**
	 * 更新实体.
	 * @param entity 实体对象
	 */
	void update(T entity);

	/**
	 * 根据 ID 删除.
	 * @param id 主键 ID
	 */
	void deleteById(ID id);

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
	Page<T> queryPageByDsl(QueryCondition condition);

	/**
	 * DSL 列表查询.
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	List<T> queryListByDsl(QueryCondition condition);

	/**
	 * 根据 DSL 条件更新.
	 * @param entity 实体对象 (非 null 字段会被更新)
	 * @param condition 更新条件 (SET 置空列 + WHERE)
	 * @return 影响行数
	 */
	int updateByDsl(T entity, UpdateCondition condition);

	/**
	 * 删除数据 (DSL 模式).
	 * @param condition WHERE 条件
	 * @return 影响行数
	 */
	int deleteByDsl(WhereCondition condition);

}
