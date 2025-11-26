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

package org.jax.snack.framework.web.config;

import java.util.Locale;

import lombok.Data;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 环境后处理器集成测试. 验证 WebConfigEnvironment 设置的配置在实际应用中生效.
 *
 * @author Jax Jiang
 */
@WebMvcTest
class WebConfigEnvironmentTests {

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void shouldUseMessageCodeWhenKeyNotFound() {
		String message = this.messageSource.getMessage("non.existent.key", null, Locale.CHINA);
		assertThat(message).isEqualTo("non.existent.key");
	}

	@Test
	void shouldExcludeNullFieldsInJackson() {
		SampleDto dto = new SampleDto();
		dto.setField1("value1");

		String json = this.jsonMapper.writeValueAsString(dto);

		assertThat(json).contains("field1");
		assertThat(json).doesNotContain("field2");
	}

	@Test
	void shouldIgnoreUnknownPropertiesInJackson() {
		String json = "{\"field1\":\"value1\",\"unknownField\":\"value\"}";

		SampleDto dto = this.jsonMapper.readValue(json, SampleDto.class);

		assertThat(dto.getField1()).isEqualTo("value1");
	}

	@Test
	void shouldNotFailOnEmptyBeansInJackson() {
		EmptyBean emptyBean = new EmptyBean();

		String json = this.jsonMapper.writeValueAsString(emptyBean);

		assertThat(json).isEqualTo("{}");
	}

	@Data
	static class SampleDto {

		private String field1;

		private String field2;

	}

	static class EmptyBean {

	}

}
