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

package org.jax.snack.framework.redis.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RedisAutoConfiguration}.
 *
 * @author Jax Jiang
 */
class RedisAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

	@Test
	void shouldRegisterRedisTemplateBean() {
		this.contextRunner.withUserConfiguration(MockRedisConfiguration.class).run((context) -> {
			assertThat(context).hasBean("redisTemplate");
			assertThat(context).getBean("redisTemplate").isInstanceOf(RedisTemplate.class);
		});
	}

	@Test
	void shouldRegisterStringRedisTemplateBean() {
		this.contextRunner.withUserConfiguration(MockRedisConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(StringRedisTemplate.class);
			assertThat(context).hasBean("stringRedisTemplate");
		});
	}

	@Test
	void shouldConfigureSerializersForRedisTemplate() {
		this.contextRunner.withUserConfiguration(MockRedisConfiguration.class).run((context) -> {
			RedisTemplate<?, ?> redisTemplate = context.getBean("redisTemplate", RedisTemplate.class);
			assertThat(redisTemplate.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
			assertThat(redisTemplate.getValueSerializer().getClass().getName())
				.isEqualTo("org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer");
			assertThat(redisTemplate.getHashKeySerializer()).isInstanceOf(StringRedisSerializer.class);
			assertThat(redisTemplate.getHashValueSerializer().getClass().getName())
				.isEqualTo("org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class MockRedisConfiguration {

		@Bean
		RedisConnectionFactory redisConnectionFactory() {
			return mock(RedisConnectionFactory.class);
		}

	}

}
