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

package org.jax.snack.framework.excel.config;

import org.jax.snack.framework.excel.enums.CsvDelimiter;
import org.jax.snack.framework.excel.enums.CsvQuote;
import org.jax.snack.framework.excel.enums.CsvRecordSeparator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Excel 配置类测试.
 *
 * @author Jax Jiang
 */
class ExcelConfigurationTests {

	@Nested
	class ExcelPropertiesTests {

		@Test
		void shouldHaveDefaultReadConfiguration() {
			ExcelProperties properties = new ExcelProperties();

			assertThat(properties.getRead()).isNotNull();
			assertThat(properties.getRead().getBatchSize()).isEqualTo(500);
			assertThat(properties.getRead().isFailFast()).isFalse();
		}

		@Test
		void shouldHaveDefaultWriteConfiguration() {
			ExcelProperties properties = new ExcelProperties();

			assertThat(properties.getWrite()).isNotNull();
		}

		@Test
		void shouldAllowCustomBatchSize() {
			ExcelProperties properties = new ExcelProperties();
			properties.getRead().setBatchSize(1000);

			assertThat(properties.getRead().getBatchSize()).isEqualTo(1000);
		}

		@Test
		void shouldAllowCustomFailFastMode() {
			ExcelProperties properties = new ExcelProperties();
			properties.getRead().setFailFast(true);

			assertThat(properties.getRead().isFailFast()).isTrue();
		}

	}

	@Nested
	class CsvPropertiesTests {

		@Test
		void shouldHaveDefaultReadConfiguration() {
			CsvProperties properties = new CsvProperties();

			assertThat(properties.getRead()).isNotNull();
			assertThat(properties.getRead().getDelimiter()).isEqualTo(CsvDelimiter.COMMA);
			assertThat(properties.getRead().getQuote()).isEqualTo(CsvQuote.DOUBLE_QUOTE);
			assertThat(properties.getRead().getRecordSeparator()).isEqualTo(CsvRecordSeparator.CRLF);
			assertThat(properties.getRead().getNullString()).isEmpty();
		}

		@Test
		void shouldHaveDefaultWriteConfiguration() {
			CsvProperties properties = new CsvProperties();

			assertThat(properties.getWrite()).isNotNull();
			assertThat(properties.getWrite().getDelimiter()).isEqualTo(CsvDelimiter.COMMA);
			assertThat(properties.getWrite().getQuote()).isEqualTo(CsvQuote.DOUBLE_QUOTE);
			assertThat(properties.getWrite().getRecordSeparator()).isEqualTo(CsvRecordSeparator.CRLF);
			assertThat(properties.getWrite().getNullString()).isEmpty();
		}

		@Test
		void shouldAllowCustomDelimiter() {
			CsvProperties properties = new CsvProperties();
			properties.getWrite().setDelimiter(CsvDelimiter.TAB);

			assertThat(properties.getWrite().getDelimiter()).isEqualTo(CsvDelimiter.TAB);
		}

		@Test
		void shouldAllowCustomQuote() {
			CsvProperties properties = new CsvProperties();
			properties.getWrite().setQuote(CsvQuote.PIPE);

			assertThat(properties.getWrite().getQuote()).isEqualTo(CsvQuote.PIPE);
		}

		@Test
		void shouldAllowCustomRecordSeparator() {
			CsvProperties properties = new CsvProperties();
			properties.getWrite().setRecordSeparator(CsvRecordSeparator.LF);

			assertThat(properties.getWrite().getRecordSeparator()).isEqualTo(CsvRecordSeparator.LF);
		}

		@Test
		void shouldAllowCustomNullString() {
			CsvProperties properties = new CsvProperties();
			properties.getWrite().setNullString("NULL");

			assertThat(properties.getWrite().getNullString()).isEqualTo("NULL");
		}

	}

}
