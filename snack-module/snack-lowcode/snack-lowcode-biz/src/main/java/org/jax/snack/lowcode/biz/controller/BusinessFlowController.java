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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jax.snack.lowcode.api.dto.FlowDesignDTO;
import org.jax.snack.lowcode.biz.liteflow.chain.ChainRefreshService;
import org.jax.snack.lowcode.biz.service.flow.FlowDesignerService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 业务流程管理控制器.
 * <p>
 * 提供流程设计、保存、发布等 API.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode/flows")
@RequiredArgsConstructor
public class BusinessFlowController {

	private final FlowDesignerService flowDesignerService;

	private final ChainRefreshService chainRefreshService;

	/**
	 * 保存流程设计.
	 * @param design 设计 DTO
	 */
	@PostMapping
	public void saveDesign(@Valid @RequestBody FlowDesignDTO design) {
		this.flowDesignerService.saveDesign(design);
	}

	/**
	 * 获取流程设计.
	 * @param flowCode 流程编码
	 * @return 设计 DTO
	 */
	@GetMapping("/{flowCode}")
	public ResponseEntity<FlowDesignDTO> getDesign(@PathVariable String flowCode) {
		FlowDesignDTO design = this.flowDesignerService.getDesign(flowCode);
		if (design == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(design);
	}

	/**
	 * 发布流程 (刷新 Chain 缓存).
	 * @param flowCode 流程编码
	 */
	@PostMapping("/{flowCode}/publish")
	public void publish(@PathVariable String flowCode) {
		this.chainRefreshService.refresh(flowCode);
	}

}
