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

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jax.snack.framework.excel.config.CsvProperties;
import org.jax.snack.framework.excel.config.ExcelExportConfig;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.config.ExcelSheetExportConfig;
import org.jax.snack.framework.excel.style.ExcelStyleFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 写入服务测试.
 *
 * @author Jax Jiang
 */
class ExcelWriteTests {

	private static final String ALICE = "Alice";

	private static final String BOB = "Bob";

	private static final String SHEET1 = "Sheet1";

	private static final String NAME_FIELD = "name";

	private ExcelWriteService writeService;

	private ExcelReadService readService;

	private ExcelBuilderFactory factory;

	@BeforeEach
	void setUp() {
		ExcelProperties excelProperties = new ExcelProperties();
		CsvProperties csvProperties = new CsvProperties();
		this.readService = new ExcelReadService(excelProperties, null);
		this.writeService = new ExcelWriteService(excelProperties, csvProperties);
		this.factory = new ExcelBuilderFactory(this.readService, this.writeService);
	}

	@Nested
	class BasicWriteTests {

		@Test
		void shouldWriteWithDefaultParameters() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ExcelWriteTests.this.factory.write(outputStream, (ctx) -> ctx.sheet(SHEET1, users, User.class));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

		@Test
		void shouldWriteWithCustomSheetName() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			String customSheetName = "User Data";

			ExcelWriteTests.this.factory.write(outputStream, (ctx) -> ctx.sheet(customSheetName, users, User.class));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

	}

	@Nested
	class ConfigDrivenTests {

		@Test
		void shouldExportByFullConfig() {
			ExcelExportConfig config = new ExcelExportConfig();
			config.setPassword("123");

			ExcelSheetExportConfig sheet1 = new ExcelSheetExportConfig();
			sheet1.setSheetName("S1");
			sheet1.setHeaders(Map.of(NAME_FIELD, List.of("姓名")));
			config.addSheet(sheet1);

			List<User> users = List.of(new User(ALICE, 25));
			Map<String, List<User>> dataMap = Map.of("S1", users);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ExcelWriteTests.this.factory.write(outputStream, (ctx) -> ctx.applyConfig(config, dataMap, User.class));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

	}

	@Nested
	class MixedModeTests {

		@Test
		void shouldExportByMixedMode() {
			List<User> users = List.of(new User(ALICE, 25));

			ExcelSheetExportConfig sheetConfig = new ExcelSheetExportConfig();
			sheetConfig.setSheetName("S_Mixed");
			sheetConfig.setAutoColumnWidth(true);

			Map<String, List<String>> headers = new LinkedHashMap<>();
			headers.put(NAME_FIELD, List.of("姓名"));
			sheetConfig.setHeaders(headers);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ExcelWriteTests.this.factory.write(outputStream, (ctx) -> ctx.customize((wb) -> wb.password("pwd"))
				.sheet(User.class, (sheetCtx) -> sheetCtx.data(users).applyConfig(sheetConfig)));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

	}

	@Nested
	class HeaderConfigurationTests {

		@Test
		void shouldWriteWithDynamicHeaders() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Map<String, List<String>> headers = Map.of(NAME_FIELD, List.of("姓名"), "age", List.of("年龄"));

			ExcelWriteTests.this.factory.write(outputStream,
					(ctx) -> ctx.sheet(SHEET1, User.class, (sheetCtx) -> sheetCtx.data(users).headers(headers)));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

	}

	@Nested
	class ChainedConfigurationTests {

		@Test
		void shouldApplyMultipleConfigurations() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			ExcelWriteTests.this.factory.write(outputStream,
					(ctx) -> ctx.style(ExcelStyleFactory.create((short) 50, (short) 10, true))
						.sheet("用户数据", User.class, (sheetCtx) -> sheetCtx.data(users)
							.headers(Map.of(NAME_FIELD, List.of("姓名"), "age", List.of("年龄")))));

			assertThat(outputStream.toByteArray()).isNotEmpty();
		}

	}

}
