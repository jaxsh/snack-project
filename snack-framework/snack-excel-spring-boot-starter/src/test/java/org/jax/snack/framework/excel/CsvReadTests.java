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
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.jax.snack.framework.excel.config.CsvProperties;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV 读取服务测试.
 *
 * @author Jax Jiang
 */
class CsvReadTests {

	private static final String ALICE = "Alice";

	private static final String BOB = "Bob";

	private ExcelReadService readService;

	private ExcelWriteService writeService;

	private ExcelBuilderFactory factory;

	@BeforeEach
	void setUp() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		ExcelProperties excelProperties = new ExcelProperties();
		this.readService = new ExcelReadService(excelProperties, validator);
		this.writeService = new ExcelWriteService(excelProperties, new CsvProperties());
		this.factory = new ExcelBuilderFactory(this.readService, this.writeService);
	}

	private ByteArrayInputStream createCsvStream(List<User> users) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		this.factory.writeCsv(outputStream, users, User.class, (ctx) -> ctx.delimiter(CsvDelimiter.COMMA).doWrite());
		return new ByteArrayInputStream(outputStream.toByteArray());
	}

	private List<User> generateUsers(int count) {
		List<User> users = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			users.add(new User("User" + i, 20 + i % 50));
		}
		return users;
	}

	@Nested
	class BasicReadTests {

		@Test
		void shouldReadCsvData() {
			List<User> users = List.of(new User(ALICE, 25), new User(BOB, 30));
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class, (ctx) -> ctx.doRead(result::addAll));

			assertThat(result).hasSize(2);
			assertThat(result.get(0).getName()).isEqualTo("Alice");
			assertThat(result.get(1).getName()).isEqualTo("Bob");
		}

		@Test
		void shouldReadEmptyCsv() {
			List<User> users = List.of();
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class, (ctx) -> ctx.doRead(result::addAll));

			assertThat(result).isEmpty();
		}

	}

	@Nested
	class BatchConfigurationTests {

		@Test
		void shouldUseDefaultBatchSize() {
			List<User> users = generateUsers(100);
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class, (ctx) -> ctx.doRead(result::addAll));

			assertThat(result).hasSize(100);
		}

		@Test
		void shouldUseCustomBatchSize() {
			List<User> users = generateUsers(100);
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class,
					(ctx) -> ctx.batchSize(50).doRead(result::addAll));

			assertThat(result).hasSize(100);
		}

	}

	@Nested
	class FailFastConfigurationTests {

		@Test
		void shouldUseDefaultFailFastMode() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class, (ctx) -> ctx.doRead(result::addAll));

			assertThat(result).hasSize(1);
		}

		@Test
		void shouldUseCustomFailFastMode() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class,
					(ctx) -> ctx.failFast(true).doRead(result::addAll));

			assertThat(result).hasSize(1);
		}

	}

	@Nested
	class ChainedConfigurationTests {

		@Test
		void shouldApplyMultipleConfigurations() {
			List<User> users = generateUsers(50);
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			CsvReadTests.this.factory.readCsv(inputStream, User.class,
					(ctx) -> ctx.batchSize(25).failFast(true).doRead(result::addAll));

			assertThat(result).hasSize(50);
		}

	}

	@Nested
	class ConfigTests {

		@Test
		void shouldReadWithCustomHeadRowNumberFromConfig() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayInputStream inputStream = createCsvStream(users);
			List<User> result = new ArrayList<>();

			org.jax.snack.framework.excel.config.ExcelReadConfig config = new org.jax.snack.framework.excel.config.ExcelReadConfig();
			config.setHeadRowNumber(1);

			CsvReadTests.this.factory.readCsv(inputStream, User.class,
					(ctx) -> ctx.applyConfig(config).doRead(result::addAll));

			assertThat(result).hasSize(1);
		}

	}

}
