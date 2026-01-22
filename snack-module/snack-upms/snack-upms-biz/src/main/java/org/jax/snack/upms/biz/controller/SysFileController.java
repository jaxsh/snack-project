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

package org.jax.snack.upms.biz.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.api.service.SysFileService;
import org.jax.snack.upms.api.vo.SysFileVO;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理 Controller.
 *
 * @author Jax Jiang
 */
@Validated
@RestController
@RequestMapping("/api/upms/files")
@RequiredArgsConstructor
public class SysFileController {

	private final SysFileService sysFileService;

	/**
	 * 上传文件.
	 * @param file 文件
	 * @return 文件信息
	 * @throws IOException IO 异常
	 * @throws NoSuchAlgorithmException 算法不存在异常
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public SysFileVO upload(@RequestParam("file") MultipartFile file) throws IOException, NoSuchAlgorithmException {
		return this.sysFileService.upload(file.getInputStream(), file.getOriginalFilename(), file.getSize());
	}

	/**
	 * 获取文件信息.
	 * @param id 文件 ID
	 * @return 文件信息
	 */
	@GetMapping("/{id}")
	public SysFileVO getById(@PathVariable Long id) {
		return this.sysFileService.getById(id);
	}

	/**
	 * 下载文件.
	 * @param id 文件 ID
	 * @return 文件资源
	 */
	@GetMapping("/{id}/download")
	public ResponseEntity<Resource> download(@PathVariable Long id) {
		SysFileVO fileVO = this.sysFileService.getById(id);
		Resource resource = this.sysFileService.download(id);

		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(fileVO.getOriginalName(), StandardCharsets.UTF_8)
			.build();

		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
			.contentType(MediaType.parseMediaType(fileVO.getContentType()))
			.body(resource);
	}

	/**
	 * 删除文件.
	 * @param id 文件 ID
	 */
	@DeleteMapping("/{id}")
	public void delete(@PathVariable Long id) {
		this.sysFileService.delete(id);
	}

}
