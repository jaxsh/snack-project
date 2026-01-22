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

package org.jax.snack.oauth.api.service;

import java.util.List;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.oauth.api.dto.RegisteredClientDTO;
import org.jax.snack.oauth.api.vo.RegisteredClientVO;

/**
 * OAuth2 客户端服务接口.
 *
 * @author Jax Jiang
 */
public interface OAuth2RegisteredClientService {

	/**
	 * 创建客户端.
	 * @param dto 客户端 DTO
	 */
	void create(RegisteredClientDTO dto);

	/**
	 * 更新客户端.
	 * @param id 主键 ID
	 * @param dto 客户端 DTO
	 */
	void update(String id, RegisteredClientDTO dto);

	/**
	 * 删除客户端.
	 * @param id 主键 ID
	 */
	void deleteById(String id);

	/**
	 * 分页查询客户端 (DSL).
	 * @param condition 查询条件
	 * @return 分页结果
	 */
	PageResult<RegisteredClientVO> queryByDsl(QueryCondition condition);

	/**
	 * 根据 Client ID 获取客户端.
	 * @param clientId 客户端 ID
	 * @return 客户端 VO
	 */
	List<RegisteredClientVO> getByClientId(String clientId);

}
