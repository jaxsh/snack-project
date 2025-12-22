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

package org.jax.snack.lowcode.biz.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 动态 CRUD Mapper.
 * <p>
 * 使用 MyBatis Plus Wrapper 驱动查询和更新，使用 XML Script 驱动插入. SQL 中的表名占位符 'lowcode_dynamic_table'
 * 将由动态表名拦截器替换.
 * </p>
 *
 * @author Jax Jiang
 */
@Mapper
public interface DynamicCrudMapper {

	// ==================== 查询 ====================

	/**
	 * 动态分页查询.
	 * @param page 分页参数
	 * @param selectColumns 查询列
	 * @param wrapper 包含 where 条件的 Wrapper
	 * @return 分页结果
	 */
	@Select("SELECT ${selectColumns} FROM lowcode_dynamic_table ${ew.customSqlSegment}")
	IPage<Map<String, Object>> dynamicSelectPage(IPage<Map<String, Object>> page,
			@Param("selectColumns") String selectColumns, @Param(Constants.WRAPPER) Wrapper<Void> wrapper);

	/**
	 * 动态查询列表.
	 * @param selectColumns 查询列
	 * @param wrapper 包含 where 条件的 Wrapper
	 * @return 数据列表
	 */
	@Select("SELECT ${selectColumns} FROM lowcode_dynamic_table ${ew.customSqlSegment}")
	List<Map<String, Object>> dynamicSelectList(@Param("selectColumns") String selectColumns,
			@Param(Constants.WRAPPER) Wrapper<Void> wrapper);

	// ==================== 插入 ====================

	/**
	 * 动态插入数据.
	 * @param data 数据 Map
	 * @return 影响行数
	 */
	@Insert("<script>" + "INSERT INTO lowcode_dynamic_table " + "<trim prefix='(' suffix=')' suffixOverrides=','>"
			+ "<foreach collection='data.keys' item='k'> `${k}`, </foreach>" + "</trim>" + " VALUES "
			+ "<trim prefix='(' suffix=')' suffixOverrides=','>"
			+ "<foreach collection='data.values' item='v'> #{v}, </foreach>" + "</trim>" + "</script>")
	@Options(useGeneratedKeys = true, keyProperty = "data.id", keyColumn = "id")
	int dynamicInsert(@Param("data") Map<String, Object> data);

	// ==================== 更新 ====================

	/**
	 * 动态更新数据 (支持 Patch 更新，即仅更新 Wrapper 中 set 的字段).
	 * @param wrapper 包含 set 语句和 where 条件的 UpdateWrapper
	 * @return 影响行数
	 */
	@Update("UPDATE lowcode_dynamic_table SET ${ew.sqlSet} ${ew.customSqlSegment}")
	int dynamicUpdate(@Param(Constants.WRAPPER) Wrapper<Void> wrapper);

	// ==================== 删除 ====================

	/**
	 * 动态删除数据 (物理删除或根据 Wrapper 条件删除).
	 * @param wrapper 包含 where 条件的 Wrapper
	 * @return 影响行数
	 */
	@Delete("DELETE FROM lowcode_dynamic_table ${ew.customSqlSegment}")
	int dynamicDelete(@Param(Constants.WRAPPER) Wrapper<Void> wrapper);

}
