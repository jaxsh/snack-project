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
import org.jax.snack.upms.biz.entity.SysOrg;

/**
 * 组织机构仓储接口.
 *
 * @author Jax Jiang
 */
public interface SysOrgRepository {

	/**
	 * DSL 存在性查询.
	 * @param condition 查询条件
	 * @return 是否存在
	 */
	boolean existsByDsl(QueryCondition condition);

	/**
	 * 保存组织机构.
	 * @param entity 组织机构实体
	 */
	void save(SysOrg entity);

	/**
	 * 根据 ID 查询.
	 * @param id 主键 ID
	 * @return Optional 包装的实体
	 */
	Optional<SysOrg> findById(Long id);

	/**
	 * 更新组织机构.
	 * @param entity 组织机构实体
	 */
	void update(SysOrg entity);

	/**
	 * 根据 ID 删除组织机构.
	 * @param id 主键 ID
	 */
	void deleteById(Long id);

	/**
	 * 批量删除组织机构.
	 * @param ids 主键 ID 列表
	 */
	void deleteByIds(List<Long> ids);

	/**
	 * DSL 分页查询.
	 * @param condition 查询条件
	 * @return MyBatis-Plus 分页对象
	 */
	Page<SysOrg> queryPageByDsl(QueryCondition condition);

	/**
	 * DSL 列表查询.
	 * @param condition 查询条件
	 * @return 实体列表
	 */
	List<SysOrg> queryListByDsl(QueryCondition condition);

	/**
	 * 批量更新子孙节点的祖先路径和层级.
	 * @param oldPrefix 旧祖先路径前缀
	 * @param newPrefix 新祖先路径前缀
	 * @param levelDiff 层级差值
	 */
	void batchUpdateDescendants(String oldPrefix, String newPrefix, int levelDiff);

	/**
	 * 批量禁用子孙节点.
	 * @param ancestorsPrefix 祖先路径前缀
	 * @param status 状态值
	 */
	void batchUpdateDescendantsStatus(String ancestorsPrefix, Integer status);

}
