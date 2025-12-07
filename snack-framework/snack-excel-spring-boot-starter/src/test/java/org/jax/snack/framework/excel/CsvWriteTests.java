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
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jax.snack.framework.excel.config.CsvProperties;
import org.jax.snack.framework.excel.config.ExcelProperties;
import org.jax.snack.framework.excel.context.CsvWriteContext;
import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.jax.snack.framework.excel.enums.CsvQuote;
import org.jax.snack.framework.excel.enums.CsvRecordSeparator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV 写入服务测试.
 *
 * @author Jax Jiang
 */
class CsvWriteTests {

	private static final String ALICE = "Alice";

	private static final String BOB = "Bob";

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
		void shouldWriteWithDefaultConfiguration() {
			List<User> users = List.of(new User(ALICE, 25), new User(BOB, 30));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			CsvWriteTests.this.factory.writeCsv(outputStream, users, User.class, CsvWriteContext::doWrite);

			String content = outputStream.toString(StandardCharsets.UTF_8);
			assertThat(content).isNotEmpty();
			assertThat(content).contains(ALICE);
			assertThat(content).contains(BOB);
		}

	}

	@Nested
	class DelimiterConfigurationTests {

		@Test
		void shouldWriteWithCommaDelimiter() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			CsvWriteTests.this.factory.writeCsv(outputStream, users, User.class,
					(ctx) -> ctx.delimiter(CsvDelimiter.COMMA).doWrite());

			String content = outputStream.toString(StandardCharsets.UTF_8);
			assertThat(content).contains("Alice,25");
		}

	}

	@Nested
	class ChainedConfigurationTests {

		@Test
		void shouldApplyMultipleConfigurations() {
			List<User> users = List.of(new User(ALICE, 25), new User(BOB, null));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			CsvWriteTests.this.factory.writeCsv(outputStream, users, User.class,
					(ctx) -> ctx.delimiter(CsvDelimiter.TAB)
						.quote(CsvQuote.PIPE)
						.recordSeparator(CsvRecordSeparator.LF)
						.nullString("NULL")
						.doWrite());

			String content = outputStream.toString(StandardCharsets.UTF_8);
			assertThat(content).isNotEmpty();
			assertThat(content).contains("Alice\t25");
			assertThat(content).contains("NULL");
		}

	}

	@Nested
	class CustomizerTests {

		@Test
		void shouldApplyCustomizer() {
			List<User> users = List.of(new User(ALICE, 25));
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			// Just verify that customizer is called without throwing exception
			CsvWriteTests.this.factory.writeCsv(outputStream, users, User.class,
					(ctx) -> ctx.customize((builder) -> builder.autoCloseStream(true)).doWrite());

			String content = outputStream.toString(StandardCharsets.UTF_8);
			assertThat(content).isNotEmpty();
		}

	}

}
