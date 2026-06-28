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

package org.jax.snack.upms.job;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.upms.biz.config.FileStorageProperties;
import org.jax.snack.upms.biz.entity.SysFile;
import org.jax.snack.upms.biz.entity.SysFileStorage;
import org.jax.snack.upms.biz.job.FileCleanupJob;
import org.jax.snack.upms.biz.repository.SysFileRepository;
import org.jax.snack.upms.biz.repository.SysFileStorageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * 文件清理定时任务 单元测试.
 *
 * @author Jax Jiang
 */
@ExtendWith(MockitoExtension.class)
public class FileCleanupJobTests {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private SysFileRepository fileRepository;

	@Mock
	private SysFileStorageRepository storageRepository;

	@Mock
	private FileStorageProperties properties;

	@Mock
	private JsonMapper jsonMapper;

	@Mock
	private TransactionTemplate transactionTemplate;

	@InjectMocks
	private FileCleanupJob fileCleanupJob;

	@Test
	void shouldCleanupOrphanFiles(@TempDir Path tempDir) throws Exception {
		JobExecutionContext context = mock(JobExecutionContext.class);
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("tables", List.of(new FileCleanupJob.TableConfig("sys_user", "avatar", false)));
		jobDataMap.put("batchSize", 100);
		given(context.getMergedJobDataMap()).willReturn(jobDataMap);

		given(this.properties.getOrphanDays()).willReturn(1);

		given(this.jsonMapper.convertValue(any(JobDataMap.class), eq(FileCleanupJob.FileCleanupConfig.class)))
			.willReturn(new FileCleanupJob.FileCleanupConfig(
					List.of(new FileCleanupJob.TableConfig("sys_user", "avatar", false)), 100));

		given(this.jdbcTemplate.queryForList(anyString(), eq(String.class))).willReturn(List.of("active.jpg"));

		SysFileStorage activeStorage = new SysFileStorage();
		activeStorage.setId(1L);
		activeStorage.setStoragePath("active.jpg");
		activeStorage.setCreateTime(ZonedDateTime.now().minusDays(2));

		SysFileStorage orphanStorage = new SysFileStorage();
		orphanStorage.setId(2L);
		orphanStorage.setStoragePath("orphan.jpg");
		orphanStorage.setCreateTime(ZonedDateTime.now().minusDays(2));

		Page<SysFileStorage> page = new Page<>();
		page.setRecords(List.of(activeStorage, orphanStorage));
		given(this.storageRepository.queryPageByDsl(any(QueryCondition.class))).willReturn(page);

		given(this.transactionTemplate.execute(any())).willAnswer((invocation) -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(mock(TransactionStatus.class));
		});

		SysFile sysFile = new SysFile();
		sysFile.setId(10L);
		given(this.fileRepository.queryListByDsl(any(QueryCondition.class))).willReturn(List.of(sysFile));

		given(this.properties.getStoragePath()).willReturn(tempDir.toString());
		Path orphanPath = tempDir.resolve("orphan.jpg");
		Files.createFile(orphanPath);

		this.fileCleanupJob.execute(context);

		then(this.storageRepository).should(never()).deleteById(1L);
		then(this.storageRepository).should().deleteById(2L);
		then(this.fileRepository).should().deleteByDsl(any());
		assertThat(Files.exists(orphanPath)).isFalse();
	}

	@Test
	void shouldSkipGracePeriodFiles() {
		FileCleanupJob.TableConfig config = new FileCleanupJob.TableConfig("sys_user", "avatar", false);

		given(this.properties.getOrphanDays()).willReturn(1);

		Page<SysFileStorage> emptyPage = new Page<>();
		emptyPage.setRecords(Collections.emptyList());

		given(this.storageRepository.queryPageByDsl(any(QueryCondition.class))).willAnswer((invocation) -> {
			QueryCondition condition = invocation.getArgument(0);
			assertThat(condition.getWhere()).containsKey(SysFileStorage.Fields.createTime);
			return emptyPage;
		});

		this.fileCleanupJob.cleanup(List.of(config), 100);
	}

	@Test
	void shouldExtractFilesFromRichText(@TempDir Path tempDir) throws IOException {
		FileCleanupJob.TableConfig config = new FileCleanupJob.TableConfig("sys_notice", "content", true);

		given(this.properties.getOrphanDays()).willReturn(1);
		given(this.properties.getUrlPrefix()).willReturn("/upload");

		String richText = """
				<p>Some text</p>
				<!-- 1. Relative Path (Double Quote): Should be kept -->
				<img src="/upload/2023/10/01/kept.jpg" />
				<!-- 2. Relative Path (Single Quote): Should be kept -->
				<img src='/upload/2023/10/01/kept_single.jpg' />
				<!-- 3. Absolute URL: Should be ignored by strict regex (even if internal) -->
				<img src="https://myserver.com/upload/2023/10/01/ignored_abs.jpg" />
				<a href="/upload/2023/10/01/also_kept.pdf">Link</a>
				""";
		given(this.jdbcTemplate.queryForList(anyString(), eq(String.class))).willReturn(List.of(richText));

		SysFileStorage keptStorage = new SysFileStorage();
		keptStorage.setId(1L);
		keptStorage.setStoragePath("2023/10/01/kept.jpg");
		keptStorage.setCreateTime(ZonedDateTime.now().minusDays(2));

		SysFileStorage keptSingleStorage = new SysFileStorage();
		keptSingleStorage.setId(2L);
		keptSingleStorage.setStoragePath("2023/10/01/kept_single.jpg");
		keptSingleStorage.setCreateTime(ZonedDateTime.now().minusDays(2));

		SysFileStorage ignoredStorage = new SysFileStorage();
		ignoredStorage.setId(3L);
		ignoredStorage.setStoragePath("2023/10/01/ignored_abs.jpg");
		ignoredStorage.setCreateTime(ZonedDateTime.now().minusDays(2));

		Page<SysFileStorage> page = new Page<>();
		page.setRecords(List.of(keptStorage, keptSingleStorage, ignoredStorage));
		given(this.storageRepository.queryPageByDsl(any(QueryCondition.class))).willReturn(page);

		given(this.transactionTemplate.execute(any())).willAnswer((invocation) -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(mock(TransactionStatus.class));
		});

		given(this.fileRepository.queryListByDsl(any(QueryCondition.class))).willReturn(Collections.emptyList());

		given(this.properties.getStoragePath()).willReturn(tempDir.toString());

		Path keptPath = tempDir.resolve("2023/10/01/kept.jpg");
		Files.createDirectories(keptPath.getParent());
		Files.createFile(keptPath);

		Path keptSinglePath = tempDir.resolve("2023/10/01/kept_single.jpg");
		Files.createFile(keptSinglePath);

		Path ignoredPath = tempDir.resolve("2023/10/01/ignored_abs.jpg");
		Files.createFile(ignoredPath);

		this.fileCleanupJob.cleanup(List.of(config), 100);

		then(this.storageRepository).should(never()).deleteById(1L);
		then(this.storageRepository).should(never()).deleteById(2L);
		then(this.storageRepository).should().deleteById(3L);
	}

}
