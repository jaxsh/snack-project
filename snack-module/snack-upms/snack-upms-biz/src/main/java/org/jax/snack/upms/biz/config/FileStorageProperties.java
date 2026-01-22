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

package org.jax.snack.upms.biz.config;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "snack.file")
public class FileStorageProperties {

	/**
	 * 存储根路径.
	 */
	private String storagePath = "/data/upload";

	/**
	 * 最大文件大小 (bytes).
	 */
	private long maxSize = 10 * 1024 * 1024;

	/**
	 * 允许的扩展名.
	 */
	private Set<String> allowedExtensions = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp", "pdf", "doc", "docx",
			"xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar");

	/**
	 * URL 前缀.
	 */
	private String urlPrefix = "/upload";

	/**
	 * 孤儿文件保留天数.
	 */
	private int orphanDays = 7;

}
