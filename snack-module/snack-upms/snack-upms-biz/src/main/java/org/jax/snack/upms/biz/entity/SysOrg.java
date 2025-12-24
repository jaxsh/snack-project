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

package org.jax.snack.upms.biz.entity;

import java.io.Serial;
import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.mybatisplus.entity.BaseEntity;

/**
 * 组织机构表.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@TableName("sys_org")
public class SysOrg extends BaseEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 业务编码 (唯一, 格式 ORG-简称-序号).
	 */
	private String orgCode;

	/**
	 * 机构名称.
	 */
	private String orgName;

	/**
	 * 简称/缩写 (英文或拼音, 用于 orgCode 前缀).
	 */
	private String shortName;

	/**
	 * 父节点编码 (根节点为空).
	 */
	private String parentCode;

	/**
	 * 层级深度 (根节点为0).
	 */
	private Integer level;

	/**
	 * 祖先路径 (存 orgCode, 如 ORG-HQ-001,ORG-SH-002).
	 */
	private String ancestors;

	/**
	 * 省.
	 */
	private String province;

	/**
	 * 市.
	 */
	private String city;

	/**
	 * 区.
	 */
	private String district;

	/**
	 * 详细地址.
	 */
	private String address;

	/**
	 * 联系人.
	 */
	private String contactName;

	/**
	 * 联系电话.
	 */
	private String contactPhone;

	/**
	 * 排序.
	 */
	private Integer sortOrder;

	/**
	 * 状态.
	 */
	private Integer status;

}
