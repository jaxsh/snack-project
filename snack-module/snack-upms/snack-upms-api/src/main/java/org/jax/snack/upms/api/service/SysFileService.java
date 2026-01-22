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

import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.jax.snack.upms.api.vo.SysFileVO;

import org.springframework.core.io.Resource;

/**
 * 文件服务接口.
 *
 * @author Jax Jiang
 */
public interface SysFileService {

	/**
	 * 上传文件.
	 * @param is 文件输入流
	 * @param originalName 原始文件名
	 * @param size 文件大小
	 * @return 文件信息
	 * @throws IOException IO 异常
	 * @throws NoSuchAlgorithmException 算法不存在异常
	 */
	SysFileVO upload(InputStream is, String originalName, long size) throws IOException, NoSuchAlgorithmException;

	/**
	 * 获取文件信息.
	 * @param id 文件 ID
	 * @return 文件信息
	 */
	SysFileVO getById(Long id);

	/**
	 * 获取文件下载资源.
	 * @param id 文件 ID
	 * @return 文件资源
	 */
	Resource download(Long id);

	/**
	 * 删除文件.
	 * @param id 文件 ID
	 */
	void delete(Long id);

}
