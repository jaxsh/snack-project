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
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 动态 CRUD 仓储接口.
 *
 * @author Jax Jiang
 */
public interface DynamicCrudRepository {

	/**
	 * 动态分页查询.
	 * @param page 分页参数
	 * @param selectColumns 查询列
	 * @param wrapper 查询条件
	 * @return 分页结果
	 */
	IPage<Map<String, Object>> selectPage(IPage<Map<String, Object>> page, String selectColumns, Wrapper<Void> wrapper);

	/**
	 * 动态列表查询.
	 * @param selectColumns 查询列
	 * @param wrapper 查询条件
	 * @return 数据列表
	 */
	List<Map<String, Object>> selectList(String selectColumns, Wrapper<Void> wrapper);

	/**
	 * 动态插入.
	 * @param data 数据 Map
	 * @return 影响行数
	 */
	int insert(Map<String, Object> data);

	/**
	 * 动态更新.
	 * @param wrapper 包含 set 语句和条件的 Wrapper
	 * @return 影响行数
	 */
	int update(Wrapper<Void> wrapper);

	/**
	 * 动态删除.
	 * @param wrapper 查询条件
	 * @return 影响行数
	 */
	int delete(Wrapper<Void> wrapper);

}
