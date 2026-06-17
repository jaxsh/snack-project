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
import java.util.Set;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.enums.YesNoStatus;
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
	 * 创建用户基本信息（仅单表操作，不同步角色与组织关系）.
	 * @param dto 用户 DTO.
	 */
	void create(SysUserDTO dto);

	/**
	 * 更新用户基本信息（仅单表操作，退化为仅主表更新，不处理关联表关系）.
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
	 * 获取用户拥有的角色编码集合.
	 * @param username 用户名
	 * @return 角色编码列表
	 */
	List<String> getUserRoles(String username);

	/**
	 * 根据用户名获取用户信息.
	 * @param username 用户名
	 * @return 用户信息 VO
	 */
	SysUserVO getByUsername(String username);

	/**
	 * 解锁用户.
	 * @param id 用户 ID
	 */
	void unlock(Long id);

	/**
	 * 重置用户密码.
	 * @param id 用户 ID
	 * @param newPassword 新密码
	 * @param initialPassword 是否标记为初始密码
	 * @param expired 是否标记为已过期
	 */
	void resetPassword(Long id, String newPassword, YesNoStatus initialPassword, YesNoStatus expired);

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
	 * 独立更新用户的角色关联关系.
	 * @param username 用户名
	 * @param roleCodes 角色编码集合
	 */
	void updateUserRoles(String username, Set<String> roleCodes);

	/**
	 * 独立更新用户的组织机构关联关系.
	 * @param username 用户名
	 * @param orgCodes 组织机构编码集合
	 */
	void updateUserOrgs(String username, Set<String> orgCodes);

	/**
	 * 生成 MFA TOTP 密钥和二维码 URI.
	 * @param username 用户名
	 * @return MFA 初始化信息
	 */
	MfaSetupVO mfaSetup(String username);

	/**
	 * 创建用户及其角色、组织关联关系 (管理员级联创建).
	 * @param dto 用户 DTO
	 */
	void createWithRelations(SysUserDTO dto);

	/**
	 * 更新用户及其角色、组织关联关系 (管理员级联更新).
	 * @param id 用户 ID
	 * @param dto 用户 DTO
	 */
	void updateWithRelations(Long id, SysUserDTO dto);

}
