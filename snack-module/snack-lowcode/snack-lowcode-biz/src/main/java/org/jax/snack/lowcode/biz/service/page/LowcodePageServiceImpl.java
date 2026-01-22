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
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.lowcode.api.dto.SavePageRequest;
import org.jax.snack.lowcode.api.service.LowcodePageService;
import org.jax.snack.lowcode.api.vo.LowcodePageVO;
import org.jax.snack.lowcode.biz.converter.LowcodePageConverter;
import org.jax.snack.lowcode.biz.entity.LowcodePage;
import org.jax.snack.lowcode.biz.repository.LowcodePageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 页面配置服务实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LowcodePageServiceImpl implements LowcodePageService {

	private final LowcodePageRepository pageRepository;

	private final LowcodePageConverter pageConverter;

	@Override
	public List<LowcodePageVO> getPages(Long schemaId) {
		QueryCondition condition = QueryCondition.builder().eq(LowcodePage.Fields.schemaId, schemaId).build();
		return this.pageRepository.queryListByDsl(condition).stream().map(this.pageConverter::toVo).toList();
	}

	@Override
	public Optional<LowcodePageVO> getPage(Long schemaId, String pageType) {
		QueryCondition condition = QueryCondition.builder()
			.eq(LowcodePage.Fields.schemaId, schemaId)
			.eq(LowcodePage.Fields.pageType, pageType)
			.build();
		List<LowcodePage> results = this.pageRepository.queryListByDsl(condition);
		return results.isEmpty() ? Optional.empty() : Optional.of(this.pageConverter.toVo(results.get(0)));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long savePage(SavePageRequest request) {
		LowcodePage page = this.pageConverter.toEntity(request);
		if (request.getId() != null) {
			page.setId(request.getId());
			this.pageRepository.update(page);
		}
		else {
			QueryCondition existsCondition = QueryCondition.builder()
				.eq(LowcodePage.Fields.schemaId, request.getSchemaId())
				.eq(LowcodePage.Fields.pageType, request.getPageType())
				.build();
			if (this.pageRepository.existsByDsl(existsCondition)) {
				throw new IllegalArgumentException("Page of this type already exists");
			}
			this.pageRepository.save(page);
		}
		return page.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deletePage(Long id) {
		this.pageRepository.deleteById(id);
	}

}
