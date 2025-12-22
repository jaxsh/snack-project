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
 * 页面配置实体.
 * <p>
 * 存储 UI Schema，与数据模型分离.
 * </p>
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ToString(callSuper = true)
@TableName("lowcode_page")
public class LowcodePage extends BaseEntity {

	/**
	 * 关联模型ID.
	 */
	private Long schemaId;

	/**
	 * 页面类型 (create, update, list, detail).
	 */
	private String pageType;

	/**
	 * 页面名称.
	 */
	private String pageName;

	/**
	 * UI Schema JSON.
	 */
	private String pageSchema;

	/**
	 * 逻辑删除.
	 */
	@TableLogic
	private Boolean deleted;

}
