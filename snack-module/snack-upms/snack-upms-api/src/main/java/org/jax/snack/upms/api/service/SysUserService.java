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
import org.jax.snack.oauth.api.dto.OAuthUserDTO;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.vo.MfaSetupVO;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jax.snack.upms.api.vo.SysSessionVO;
import org.jax.snack.upms.api.vo.SysUserVO;

/**
 * 用户服务接口.
 *
 * @author Jax Jiang
 */
public interface SysUserService {

	/**
	 * 创建用户及其角色、组织关联关系.
	 * @param dto 用户 DTO.
	 */
	void create(SysUserDTO dto);

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
	 * 更新用户及其角色、组织关联关系.
	 * @param id 用户 ID
	 * @param dto 用户 DTO
	 */
	void update(Long id, SysUserDTO dto);

	/**
	 * 更新 oauth_user 字段（解锁、改密等动作均走此方法）.
	 * @param username 用户名
	 * @param dto 仅需设置要修改的字段，其余留 null
	 */
	void updateOAuth(String username, OAuthUserDTO dto);

	/**
	 * 强制下线用户（吊销所有 token）.
	 * @param id 用户 ID
	 */
	void revokeTokens(Long id);

	/**
	 * 查询用户的活跃 Session 列表.
	 * @param id 用户 ID
	 * @return Session 列表
	 */
	List<SysSessionVO> getSessions(Long id);

	/**
	 * 踢出用户的指定 Session.
	 * @param id 用户 ID
	 * @param sessionId Session ID
	 */
	void revokeSession(Long id, String sessionId);

	/**
	 * 获取用户已启用的角色编码列表（有缓存）.
	 * @param username 用户名
	 * @return 角色编码列表
	 */
	List<String> getEnabledRoleCodesByUsername(String username);

	/**
	 * 获取用户拥有的资源集合（按角色组合，去重）.
	 * @param username 用户名
	 * @return 资源 VO 列表
	 */
	List<SysResourceVO> getResourcesByUsername(String username);

	/**
	 * 获取用户拥有的资源集合（按角色组合，去重）.
	 * @param username 用户名
	 * @return 资源 VO 列表
	 */
	List<SysResourceVO> getUserResources(String username);

	/**
	 * 根据用户名查询用户信息，不存在时抛 DATA_NOT_FOUND.
	 * @param username 用户名
	 * @return 用户 VO
	 */
	SysUserVO findByUsername(String username);

	/**
	 * 生成 MFA TOTP 密钥和二维码 URI.
	 * @param username 用户名
	 * @return MFA 初始化信息
	 */
	MfaSetupVO mfaSetup(String username);

}
