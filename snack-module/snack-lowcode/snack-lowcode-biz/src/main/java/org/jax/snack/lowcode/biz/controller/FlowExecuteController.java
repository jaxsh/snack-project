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

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.lowcode.biz.liteflow.executor.FlowExecutorService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流程执行控制器.
 * <p>
 * 提供业务流程执行入口.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode/flow")
@RequiredArgsConstructor
public class FlowExecuteController {

	private final FlowExecutorService flowExecutorService;

	/**
	 * 执行业务流程.
	 * @param flowCode 流程编码
	 * @param data 业务数据
	 * @return 执行结果
	 */
	@PostMapping("/{flowCode}")
	public Object execute(@PathVariable String flowCode, @RequestBody Map<String, Object> data) {
		return this.flowExecutorService.execute(flowCode, data);
	}

	/**
	 * 执行业务流程 (带 ID, 用于 UPDATE/DELETE).
	 * @param flowCode 流程编码
	 * @param id 主键 ID
	 * @param data 业务数据
	 * @return 执行结果
	 */
	@PostMapping("/{flowCode}/{id}")
	public Object executeWithId(@PathVariable String flowCode, @PathVariable Long id,
			@RequestBody Map<String, Object> data) {
		return this.flowExecutorService.execute(flowCode, data, id);
	}

}
