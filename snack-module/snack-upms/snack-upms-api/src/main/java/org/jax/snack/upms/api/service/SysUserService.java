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

import java.util.List;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysUserVO;

/**
 * 用户服务接口.
 *
 * @author Jax Jiang
 */
public interface SysUserService {

	/**
	 * 创建用户.
	 * @param dto 用户 DTO.
	 */
	void create(SysUserDTO dto);

	/**
	 * 更新用户.
	 * @param id 主键 ID.
	 * @param dto 用户 DTO.
	 */
	void update(Long id, SysUserDTO dto);

	/**
	 * 根据条件删除用户.
	 * @param condition 删除条件
	 */
	void deleteByDsl(WhereCondition condition);

	/**
	 * 使用 JSON DSL 查询用户.
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<SysUserVO> queryByDsl(QueryCondition condition);

	/**
	 * 获取用户拥有的资源集合.
	 * @param username 用户名
	 * @return 资源 VO 列表
	 */
	List<SysResourceVO> getUserResources(String username);

	/**
	 * 根据 ID 获取用户信息.
	 * @param id 用户 ID
	 * @return 用户信息 VO
	 */
	SysUserVO queryById(Long id);

	/**
	 * 根据用户名获取用户信息.
	 * @param username 用户名
	 * @return 用户信息 VO
	 */
	SysUserVO getByUsername(String username);

}
