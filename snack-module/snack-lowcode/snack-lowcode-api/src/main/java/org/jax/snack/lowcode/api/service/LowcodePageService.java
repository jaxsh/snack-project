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

package org.jax.snack.lowcode.api.service;

import java.util.List;
import java.util.Optional;

import org.jax.snack.lowcode.api.dto.SavePageRequest;
import org.jax.snack.lowcode.api.vo.LowcodePageVO;

/**
 * 页面配置服务接口.
 *
 * @author Jax Jiang
 */
public interface LowcodePageService {

	/**
	 * 获取指定 Schema 的所有页面配置.
	 * @param schemaId 模型ID
	 * @return 页面 VO 列表
	 */
	List<LowcodePageVO> getPages(Long schemaId);

	/**
	 * 获取特定类型的页面配置.
	 * @param schemaId 模型ID
	 * @param pageType 页面类型
	 * @return 页面 VO (Optional)
	 */
	Optional<LowcodePageVO> getPage(Long schemaId, String pageType);

	/**
	 * 保存或更新页面配置.
	 * @param request 页面请求
	 * @return 保存后的ID
	 */
	Long savePage(SavePageRequest request);

	/**
	 * 删除页面配置.
	 * @param id 页面ID
	 */
	void deletePage(Long id);

}
