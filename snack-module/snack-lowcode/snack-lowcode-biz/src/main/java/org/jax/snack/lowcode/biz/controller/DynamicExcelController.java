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

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jax.snack.lowcode.biz.service.excel.DynamicExcelService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 动态 Excel 控制器.
 * <p>
 * 提供基于 Schema 的 Excel 导入导出接口.
 * </p>
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/lowcode")
@RequiredArgsConstructor
public class DynamicExcelController {

	private final DynamicExcelService dynamicExcelService;

	/**
	 * 导出模板.
	 * @param schemaName Schema 名称
	 * @param response HTTP 响应
	 * @throws IOException IO 异常
	 */
	@GetMapping("/{schemaName}/template")
	public void exportTemplate(@PathVariable String schemaName, HttpServletResponse response) throws IOException {
		this.dynamicExcelService.exportTemplate(schemaName, response);
	}

	/**
	 * 导出数据.
	 * @param schemaName Schema 名称
	 * @param response HTTP 响应
	 * @throws IOException IO 异常
	 */
	@PostMapping("/{schemaName}/export")
	public void exportData(@PathVariable String schemaName, HttpServletResponse response) throws IOException {
		this.dynamicExcelService.exportData(schemaName, response);
	}

	/**
	 * 导入数据.
	 * @param schemaName Schema 名称
	 * @param file Excel 文件
	 * @return 导入结果
	 * @throws IOException IO 异常
	 */
	@PostMapping("/{schemaName}/import")
	public Map<String, Object> importData(@PathVariable String schemaName, @RequestParam("file") MultipartFile file)
			throws IOException {
		return this.dynamicExcelService.importData(schemaName, file);
	}

}
