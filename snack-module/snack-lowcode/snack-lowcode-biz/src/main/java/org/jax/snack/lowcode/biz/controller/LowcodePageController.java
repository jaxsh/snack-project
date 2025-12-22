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

package org.jax.snack.lowcode.biz.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.lowcode.api.dto.SavePageRequest;
import org.jax.snack.lowcode.api.vo.LowcodePageVO;
import org.jax.snack.lowcode.biz.service.page.LowcodePageService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 页面配置控制器.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode/pages")
@RequiredArgsConstructor
public class LowcodePageController {

	private final LowcodePageService pageService;

	/**
	 * 获取 Schema 下的所有页面.
	 * @param schemaId Schema ID
	 * @return 页面列表
	 */
	@GetMapping
	public List<LowcodePageVO> list(@RequestParam Long schemaId) {
		return this.pageService.getPages(schemaId);
	}

	/**
	 * 获取指定类型的页面.
	 * @param schemaId Schema ID
	 * @param type 页面类型
	 * @return 页面配置
	 */
	@GetMapping("/{schemaId}/{type}")
	public ResponseEntity<LowcodePageVO> get(@PathVariable Long schemaId, @PathVariable String type) {
		return this.pageService.getPage(schemaId, type)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * 保存页面配置.
	 * @param request 保存请求
	 * @return 页面ID
	 */
	@PostMapping
	public Long save(@RequestBody SavePageRequest request) {
		return this.pageService.savePage(request);
	}

	/**
	 * 删除页面配置.
	 * @param id 页面ID
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		this.pageService.deletePage(id);
	}

}
