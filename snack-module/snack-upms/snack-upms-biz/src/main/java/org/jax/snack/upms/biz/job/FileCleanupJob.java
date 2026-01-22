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

package org.jax.snack.upms.biz.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.upms.biz.config.FileStorageProperties;
import org.jax.snack.upms.biz.entity.SysFile;
import org.jax.snack.upms.biz.entity.SysFileStorage;
import org.jax.snack.upms.biz.repository.SysFileRepository;
import org.jax.snack.upms.biz.repository.SysFileStorageRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 文件清理定时任务.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupJob implements Job {

	private static final String PARAM_TABLES = "tables";

	private static final String PARAM_BATCH_SIZE = "batchSize";

	private static final int DEFAULT_BATCH_SIZE = 1000;

	private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

	private final JdbcTemplate jdbcTemplate;

	private final SysFileRepository fileRepository;

	private final SysFileStorageRepository storageRepository;

	private final FileStorageProperties properties;

	private final JsonMapper jsonMapper;

	private final TransactionTemplate transactionTemplate;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		String tablesJson = context.getMergedJobDataMap().getString(PARAM_TABLES);
		if (!StringUtils.hasText(tablesJson)) {
			log.warn("File cleanup job skipped: 'tables' parameter is empty.");
			return;
		}

		int batchSize = DEFAULT_BATCH_SIZE;
		if (context.getMergedJobDataMap().containsKey(PARAM_BATCH_SIZE)) {
			batchSize = context.getMergedJobDataMap().getIntValue(PARAM_BATCH_SIZE);
		}

		try {
			List<TableConfig> configs = this.jsonMapper.readValue(tablesJson, new TypeReference<>() {
			});
			cleanup(configs, batchSize);
		}
		catch (JacksonException ex) {
			log.error("Failed to parse 'tables' parameter: {}", tablesJson, ex);
			throw new JobExecutionException(ex);
		}
	}

	public void cleanup(List<TableConfig> configs, int batchSize) {
		log.info("Starting file cleanup job with batchSize={}...", batchSize);
		Set<String> activePaths = new HashSet<>();

		for (TableConfig config : configs) {
			if (!VALID_NAME_PATTERN.matcher(config.table()).matches()
					|| !VALID_NAME_PATTERN.matcher(config.column()).matches()) {
				log.warn("Ignored invalid table config: {}", config);
				continue;
			}
			try {
				String sql = "SELECT DISTINCT " + config.column() + " FROM " + config.table() + " WHERE "
						+ config.column() + " IS NOT NULL";
				List<String> results = this.jdbcTemplate.queryForList(sql, String.class);

				if (config.richText()) {
					String prefix = this.properties.getUrlPrefix();
					Pattern pattern = Pattern.compile("([\"'])(" + Pattern.quote(prefix) + "/[\\w./-]+)\\1");
					for (String content : results) {
						if (!StringUtils.hasText(content)) {
							continue;
						}
						java.util.regex.Matcher matcher = pattern.matcher(content);
						while (matcher.find()) {
							String fullMatch = matcher.group(2);
							String storagePath = fullMatch.substring(prefix.length() + 1);
							activePaths.add(storagePath);
						}
					}
				}
				else {
					activePaths.addAll(results);
				}
			}
			catch (DataAccessException ex) {
				log.error("Failed to query table: {}", config.table(), ex);
			}
		}

		log.info("Found {} active files from business tables.", activePaths.size());

		ZonedDateTime threshold = ZonedDateTime.now().minusDays(this.properties.getOrphanDays());
		long lastId = 0L;
		int totalDeleted = 0;

		while (true) {
			QueryCondition condition = QueryCondition.builder()
				.page(1, batchSize)
				.lt(SysFileStorage.Fields.createTime, threshold)
				.gtIf(lastId > 0, SysFileStorage.Fields.id, lastId)
				.orderByAsc(SysFileStorage.Fields.id)
				.build();

			Page<SysFileStorage> page = this.storageRepository.queryPageByDsl(condition);
			List<SysFileStorage> records = page.getRecords();

			if (ObjectUtils.isEmpty(records)) {
				break;
			}

			for (SysFileStorage storage : records) {
				if (!activePaths.contains(storage.getStoragePath())) {
					boolean dbDeleted = deleteDbRecord(storage);
					if (dbDeleted) {
						deletePhysicalFile(storage);
						totalDeleted++;
					}
				}
				lastId = storage.getId();
			}

			if (records.size() < batchSize) {
				break;
			}
		}

		log.info("File cleanup job finished. Deleted {} orphan files.", totalDeleted);
	}

	private boolean deleteDbRecord(SysFileStorage storage) {
		Boolean result = this.transactionTemplate.execute((status) -> {
			try {
				QueryCondition fileCondition = QueryCondition.builder()
					.eq(SysFile.Fields.storageId, storage.getId())
					.build();
				List<SysFile> files = this.fileRepository.queryListByDsl(fileCondition);

				if (!files.isEmpty()) {
					QueryCondition deleteCond = QueryCondition.builder()
						.in(SysFile.Fields.id, files.stream().map(SysFile::getId).toList())
						.build();
					this.fileRepository.deleteByDsl(deleteCond);
				}
				this.storageRepository.deleteById(storage.getId());
				return true;
			}
			catch (DataAccessException ex) {
				log.error("Failed to delete database record for storageId: {}", storage.getId(), ex);
				status.setRollbackOnly();
				return false;
			}
		});
		return Boolean.TRUE.equals(result);
	}

	private void deletePhysicalFile(SysFileStorage storage) {
		try {
			Path path = Paths.get(this.properties.getStoragePath(), storage.getStoragePath().split("/"));
			Files.deleteIfExists(path);
			deleteEmptyParentDirectory(path.getParent());
		}
		catch (IOException ex) {
			log.error("Failed to delete physical file: {}", storage.getStoragePath(), ex);
		}
	}

	private void deleteEmptyParentDirectory(Path directory) {
		if (ObjectUtils.isEmpty(directory) || !Files.exists(directory) || !Files.isDirectory(directory)) {
			return;
		}
		try (Stream<Path> entries = Files.list(directory)) {
			if (entries.findAny().isEmpty()) {
				Files.delete(directory);
			}
		}
		catch (IOException ex) {
			log.debug("Failed to delete empty directory: {}, ex: {}", directory, ex.getMessage());
		}
	}

	public record TableConfig(String table, String column, boolean richText) {
	}

}
