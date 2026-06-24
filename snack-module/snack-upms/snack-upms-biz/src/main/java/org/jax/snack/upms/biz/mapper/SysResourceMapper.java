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

package org.jax.snack.upms.biz.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jax.snack.upms.biz.entity.SysResource;

/**
 * 资源 Mapper.
 *
 * @author Jax Jiang
 */
public interface SysResourceMapper extends BaseMapper<SysResource> {

	/**
	 * 根据角色编码列表查询资源，status 可选（null 时不过滤）.
	 * @param roleCodes 角色编码列表
	 * @param status 资源状态（可为 null）
	 * @return 资源列表
	 */
	List<SysResource> selectResourcesByRoleCodes(@Param("roleCodes") List<String> roleCodes,
			@Param("status") Integer status);

	/**
	 * 根据用户名查询资源，status 可选（null 时不过滤）.
	 * @param username 用户名
	 * @param status 资源/角色状态（可为 null）
	 * @return 资源列表
	 */
	List<SysResource> selectResourcesByUsername(@Param("username") String username, @Param("status") Integer status);

}
