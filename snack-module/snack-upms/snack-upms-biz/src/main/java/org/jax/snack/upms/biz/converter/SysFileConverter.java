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

package org.jax.snack.upms.biz.converter;

import org.jax.snack.upms.api.vo.SysFileVO;
import org.jax.snack.upms.biz.entity.SysFile;
import org.jax.snack.upms.biz.entity.SysFileStorage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 文件对象转换器.
 *
 * @author Jax Jiang
 */
@Mapper(componentModel = "spring")
public interface SysFileConverter {

	/**
	 * 转换为 VO.
	 * @param file 逻辑文件
	 * @param storage 物理文件
	 * @param url 访问 URL
	 * @return VO
	 */
	@Mapping(target = "id", source = "file.id")
	@Mapping(target = "originalName", source = "file.originalName")
	@Mapping(target = "url", source = "url")
	@Mapping(target = "fileSize", source = "storage.fileSize")
	@Mapping(target = "contentType", source = "storage.contentType")
	@Mapping(target = "extension", source = "storage.extension")
	SysFileVO toVO(SysFile file, SysFileStorage storage, String url);

}
