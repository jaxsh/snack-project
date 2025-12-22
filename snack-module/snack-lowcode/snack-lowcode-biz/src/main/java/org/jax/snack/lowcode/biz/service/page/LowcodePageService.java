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

package org.jax.snack.lowcode.biz.service.page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.lowcode.api.dto.SavePageRequest;
import org.jax.snack.lowcode.api.vo.LowcodePageVO;
import org.jax.snack.lowcode.biz.converter.LowcodePageConverter;
import org.jax.snack.lowcode.biz.entity.LowcodePage;
import org.jax.snack.lowcode.biz.repository.LowcodePageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 页面配置服务.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodePageService {

	private final LowcodePageRepository pageRepository;

	private final LowcodePageConverter pageConverter;

	/**
	 * 获取指定 Schema 的所有页面配置.
	 * @param schemaId 模型ID
	 * @return 页面 VO 列表
	 */
	public List<LowcodePageVO> getPages(Long schemaId) {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("schemaId", Map.of(QueryOperator.EQ.getValue(), schemaId)));
		return this.pageRepository.queryListByDsl(condition).stream().map(this.pageConverter::toVo).toList();
	}

	/**
	 * 获取特定类型的页面配置.
	 * @param schemaId 模型ID
	 * @param pageType 页面类型
	 * @return 页面 VO (Optional)
	 */
	public Optional<LowcodePageVO> getPage(Long schemaId, String pageType) {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("schemaId", Map.of(QueryOperator.EQ.getValue(), schemaId), "pageType",
				Map.of(QueryOperator.EQ.getValue(), pageType)));
		List<LowcodePage> results = this.pageRepository.queryListByDsl(condition);
		return results.isEmpty() ? Optional.empty() : Optional.of(this.pageConverter.toVo(results.get(0)));
	}

	/**
	 * 保存或更新页面配置.
	 * @param request 页面请求
	 * @return 保存后的ID
	 */
	@Transactional
	public Long savePage(SavePageRequest request) {
		LowcodePage page = this.pageConverter.toEntity(request);
		if (request.getId() != null) {
			page.setId(request.getId());
			this.pageRepository.update(page);
		}
		else {
			// 检查唯一性
			QueryCondition existsCondition = new QueryCondition();
			existsCondition.setWhere(Map.of("schemaId", Map.of(QueryOperator.EQ.getValue(), request.getSchemaId()),
					"pageType", Map.of(QueryOperator.EQ.getValue(), request.getPageType())));
			if (this.pageRepository.existsByDsl(existsCondition)) {
				throw new IllegalArgumentException("该类型的页面已存在");
			}
			this.pageRepository.save(page);
		}
		return page.getId();
	}

	/**
	 * 删除页面配置.
	 * @param id 页面ID
	 */
	@Transactional
	public void deletePage(Long id) {
		this.pageRepository.deleteById(id);
	}

}
