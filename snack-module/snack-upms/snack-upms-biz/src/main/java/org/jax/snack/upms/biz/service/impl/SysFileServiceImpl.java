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

package org.jax.snack.upms.biz.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.upms.api.service.SysFileService;
import org.jax.snack.upms.api.vo.SysFileVO;
import org.jax.snack.upms.biz.config.FileStorageProperties;
import org.jax.snack.upms.biz.converter.SysFileConverter;
import org.jax.snack.upms.biz.entity.SysFile;
import org.jax.snack.upms.biz.entity.SysFileStorage;
import org.jax.snack.upms.biz.repository.SysFileRepository;
import org.jax.snack.upms.biz.repository.SysFileStorageRepository;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 文件服务实现.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl implements SysFileService {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

	private final SysFileRepository fileRepository;

	private final SysFileStorageRepository storageRepository;

	private final SysFileConverter converter;

	private final FileStorageProperties properties;

	/**
	 * 上传文件.
	 * @param is 文件输入流
	 * @param originalName 原始文件名
	 * @param size 文件大小
	 * @return 文件信息
	 * @throws IOException IO 异常
	 * @throws NoSuchAlgorithmException 算法不存在异常
	 */
	@Override
	@Transactional
	public SysFileVO upload(InputStream is, String originalName, long size)
			throws IOException, NoSuchAlgorithmException {
		String extension = getExtension(originalName);
		validateExtension(extension);
		validateFileSize(size);

		try (is) {
			byte[] content = is.readAllBytes();
			String hash = calculateHash(content);
			SysFileStorage storage = findOrCreateStorage(content, hash, extension);

			SysFile sysFile = new SysFile();
			sysFile.setStorageId(storage.getId());
			sysFile.setOriginalName(originalName);
			sysFile.setAccessLevel(0);
			this.fileRepository.save(sysFile);

			String url = this.properties.getUrlPrefix() + "/" + storage.getStoragePath();
			return this.converter.toVO(sysFile, storage, url);
		}
	}

	@Override
	public SysFileVO getById(Long id) {
		SysFile file = this.fileRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
		SysFileStorage storage = this.storageRepository.findById(file.getStorageId())
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

		String url = this.properties.getUrlPrefix() + "/" + storage.getStoragePath();
		return this.converter.toVO(file, storage, url);
	}

	@Override
	public Resource download(Long id) {
		SysFile file = this.fileRepository.findById(id)
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));
		SysFileStorage storage = this.storageRepository.findById(file.getStorageId())
			.orElseThrow(() -> new BusinessException(ErrorCode.DATA_NOT_FOUND));

		Path path = Paths.get(this.properties.getStoragePath(), storage.getStoragePath().split("/"));
		if (!Files.exists(path)) {
			throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
		}
		return new FileSystemResource(path);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		this.fileRepository.deleteById(id);
	}

	private SysFileStorage findOrCreateStorage(byte[] content, String hash, String extension) {
		QueryCondition condition = QueryCondition.builder().eq(SysFileStorage.Fields.hash, hash).build();

		List<SysFileStorage> existing = this.storageRepository.queryListByDsl(condition);
		if (!existing.isEmpty()) {
			return existing.get(0);
		}

		String datePath = LocalDate.now().format(DATE_FORMATTER);
		String fileName = UUID.randomUUID() + "." + extension;
		String storagePath = datePath + "/" + fileName;

		Path root = Paths.get(this.properties.getStoragePath());
		Path fullPath = root.resolve(datePath.replace("/", File.separator)).resolve(fileName).toAbsolutePath();

		try {
			Files.createDirectories(fullPath.getParent());
			Files.write(fullPath, content);

			String actualContentType = detectContentType(fullPath);

			try {
				validateContentType(actualContentType, extension);
			}
			catch (BusinessException ex) {
				Files.deleteIfExists(fullPath);
				throw ex;
			}

			SysFileStorage storage = new SysFileStorage();
			storage.setHash(hash);
			storage.setFileSize((long) content.length);
			storage.setStoragePath(storagePath);
			storage.setContentType(actualContentType);
			storage.setExtension(extension);
			this.storageRepository.save(storage);

			return storage;
		}
		catch (IOException | IllegalStateException ex) {
			log.error("File storage failed", ex);
			throw new BusinessException(ErrorCode.SYSTEM_ERROR, "File storage failed");
		}
	}

	private String getExtension(String filename) {
		return StringUtils.getFilenameExtension(filename);
	}

	private void validateExtension(String extension) {
		if (!StringUtils.hasText(extension)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID);
		}
		if (!this.properties.getAllowedExtensions().contains(extension.toLowerCase(Locale.ROOT))) {
			String allowed = String.join(", ", this.properties.getAllowedExtensions());
			throw new BusinessException(ErrorCode.FILE_EXTENSION_NOT_ALLOWED, extension, allowed);
		}
	}

	private void validateFileSize(long size) {
		if (size > this.properties.getMaxSize()) {
			long maxMb = this.properties.getMaxSize() / 1024 / 1024;
			throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, maxMb);
		}
	}

	private String detectContentType(Path path) {
		try {
			String contentType = Files.probeContentType(path);
			if (StringUtils.hasText(contentType)) {
				return contentType;
			}
		}
		catch (IOException ex) {
			log.debug("Failed to probe content type for file: {}", path, ex);
		}
		String extension = StringUtils.getFilenameExtension(path.getFileName().toString());
		if (ObjectUtils.isEmpty(extension)) {
			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
		String prefix = getExpectedMimePrefix(extension.toLowerCase(Locale.ROOT));
		if (ObjectUtils.isEmpty(prefix)) {
			return MediaType.APPLICATION_OCTET_STREAM_VALUE;
		}
		if (!prefix.endsWith("/")) {
			return prefix;
		}
		return prefix + extension.toLowerCase(Locale.ROOT);
	}

	private void validateContentType(String contentType, String extension) {
		if (!StringUtils.hasText(contentType)) {
			return;
		}
		String expectedPrefix = getExpectedMimePrefix(extension.toLowerCase(Locale.ROOT));
		if (!ObjectUtils.isEmpty(expectedPrefix) && !contentType.toLowerCase(Locale.ROOT).startsWith(expectedPrefix)) {
			throw new BusinessException(ErrorCode.PARAM_INVALID, "Content-Type mismatch for extension: " + extension);
		}
	}

	private String getExpectedMimePrefix(String extension) {
		return switch (extension) {
			case "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/";
			case "pdf" -> "application/pdf";
			case "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip", "rar" -> "application/";
			case "txt" -> "text/";
			default -> null;
		};
	}

	/**
	 * 计算文件哈希值.
	 * @param content 文件字节数组
	 * @return 哈希字符串
	 * @throws NoSuchAlgorithmException 算法不存在异常
	 */
	private String calculateHash(byte[] content) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		return HexFormat.of().formatHex(digest.digest(content));
	}

}
