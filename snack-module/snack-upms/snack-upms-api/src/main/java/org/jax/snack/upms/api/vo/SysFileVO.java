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

package org.jax.snack.upms.api.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 文件信息 VO.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class SysFileVO {

	/**
	 * 文件 ID.
	 */
	private Long id;

	/**
	 * 访问 URL.
	 */
	private String url;

	/**
	 * 原始文件名.
	 */
	private String originalName;

	/**
	 * 文件大小 (bytes).
	 */
	private Long fileSize;

	/**
	 * MIME 类型.
	 */
	private String contentType;

	/**
	 * 文件扩展名.
	 */
	private String extension;

}
