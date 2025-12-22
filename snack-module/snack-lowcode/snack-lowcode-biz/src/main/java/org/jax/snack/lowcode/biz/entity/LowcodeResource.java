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

package org.jax.snack.lowcode.biz.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 资源映射实体.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString(callSuper = true)
@TableName("lowcode_resource")
public class LowcodeResource extends BaseEntity {

	/**
	 * API 资源路径.
	 */
	private String resourcePath;

	/**
	 * 资源类型.
	 */
	private String resourceType;

	/**
	 * 关联的 Schema ID.
	 */
	private Long schemaId;

	/**
	 * 启用列表查询.
	 */
	private Boolean enableList;

	/**
	 * 启用创建.
	 */
	private Boolean enableCreate;

	/**
	 * 启用更新.
	 */
	private Boolean enableUpdate;

	/**
	 * 启用删除.
	 */
	private Boolean enableDelete;

	/**
	 * 启用导出.
	 */
	private Boolean enableExport;

	/**
	 * 启用导入.
	 */
	private Boolean enableImport;

	/**
	 * 逻辑删除标记.
	 */
	@TableLogic
	private Integer deleted;

}
