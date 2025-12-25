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

package org.jax.snack.framework.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.idev.excel.EasyExcel;
import org.jax.snack.framework.excel.config.CsvProperties;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.config.ExcelReadConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 读取服务测试.
 *
 * @author Jax Jiang
 */
class ExcelReadTests {

	private ExcelBuilderFactory factory;

	@BeforeEach
	void setUp() {
		ExcelProperties excelProperties = new ExcelProperties();
		CsvProperties csvProperties = new CsvProperties();
		ExcelReadService readService = new ExcelReadService(excelProperties, null);
		ExcelWriteService writeService = new ExcelWriteService(excelProperties, csvProperties);
		this.factory = new ExcelBuilderFactory(readService, writeService);
	}

	private byte[] createExcelBytes(List<User> users, String sheetName) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		EasyExcel.write(outputStream, User.class).sheet(sheetName).doWrite(users);
		return outputStream.toByteArray();
	}

	@Nested
	class BasicReadTests {

		@Test
		void shouldReadExcelData() throws Exception {
			List<User> users = List.of(new User("BasicUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "Sheet1"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class, (ctx) -> ctx.doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(1);
				assertThat(readUsers.get(0).getName()).isEqualTo("BasicUser");
			}
		}

		@Test
		void shouldReadEmptyExcel() throws Exception {
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(List.of(), "SheetEmpty"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class, (ctx) -> ctx.doRead(readUsers::addAll));

				assertThat(readUsers).isEmpty();
			}
		}

	}

	@Nested
	class BatchConfigurationTests {

		@Test
		void shouldUseDefaultBatchSize() throws Exception {
			List<User> users = List.of(new User("BatchUser1", 25), new User("BatchUser2", 30));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetBatch"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class, (ctx) -> ctx.doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(2);
			}
		}

		@Test
		void shouldUseCustomBatchSize() throws Exception {
			List<User> users = List.of(new User("CustomBatch1", 25), new User("CustomBatch2", 30));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetCustomBatch"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.batchSize(1).doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(2);
			}
		}

	}

	@Nested
	class FailFastConfigurationTests {

		@Test
		void shouldUseDefaultFailFastMode() throws Exception {
			List<User> users = List.of(new User("FailUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetFail"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class, (ctx) -> ctx.doRead(readUsers::addAll));

				assertThat(readUsers).isNotEmpty();
			}
		}

		@Test
		void shouldUseCustomFailFastMode() throws Exception {
			List<User> users = List.of(new User("NoFailUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetNoFail"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.failFast(false).doRead(readUsers::addAll));

				assertThat(readUsers).isNotEmpty();
			}
		}

	}

	@Nested
	class ChainedConfigurationTests {

		@Test
		void shouldApplyMultipleConfigurations() throws Exception {
			List<User> users = List.of(new User("MultiUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetMulti"))) {
				List<User> readUsers = new ArrayList<>();
				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.batchSize(10).failFast(true).doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(1);
			}
		}

	}

	@Nested
	class SheetConfigurationTests {

		@Test
		void shouldReadSpecificSheetByName() throws Exception {
			List<User> users = List.of(new User("NamedSheetUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "TargetSheet"))) {
				List<User> readUsers = new ArrayList<>();

				ExcelReadConfig config = new ExcelReadConfig();
				config.setSheetName("TargetSheet");

				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.applyConfig(config).doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(1);
			}
		}

		@Test
		void shouldReadSpecificSheetByIndex() throws Exception {
			List<User> users = List.of(new User("IndexSheetUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "Sheet0"))) {
				List<User> readUsers = new ArrayList<>();

				ExcelReadConfig config = new ExcelReadConfig();
				config.setSheetNo(0);

				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.applyConfig(config).doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(1);
			}
		}

		@Test
		void shouldReadWithCustomHeadRowNumber() throws Exception {
			List<User> users = List.of(new User("HeadUser", 25));
			try (InputStream inputStream = new ByteArrayInputStream(createExcelBytes(users, "SheetHead"))) {
				List<User> readUsers = new ArrayList<>();

				ExcelReadConfig config = new ExcelReadConfig();
				config.setHeadRowNumber(1);

				ExcelReadTests.this.factory.read(inputStream, User.class,
						(ctx) -> ctx.applyConfig(config).doRead(readUsers::addAll));

				assertThat(readUsers).hasSize(1);
			}
		}

	}

}
