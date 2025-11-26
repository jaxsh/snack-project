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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 自动配置类.
 *
 * <p>
 * 提供 {@link RedisTemplate} 和 {@link StringRedisTemplate} Bean 的自动配置,
 * 统一了序列化策略以增强数据可读性和兼容性.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
public class RedisAutoConfiguration {

	/**
	 * 创建并配置一个 {@link RedisTemplate} Bean.
	 *
	 * <p>
	 * 核心设计决策如下:
	 * <ul>
	 * <li>键 (Key) 序列化: 使用 {@link RedisSerializer#string()}.</li>
	 * <li>值 (Value) 序列化: 使用 {@link RedisSerializer#json()}, 自动处理类型信息.</li>
	 * </ul>
	 * @param redisConnectionFactory Redis 连接工厂.
	 * @return 一个配置好的 RedisTemplate 实例.
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);

		redisTemplate.setKeySerializer(RedisSerializer.string());
		redisTemplate.setHashKeySerializer(RedisSerializer.string());

		redisTemplate.setValueSerializer(RedisSerializer.json());
		redisTemplate.setHashValueSerializer(RedisSerializer.json());

		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	/**
	 * 创建并配置一个 {@link StringRedisTemplate} Bean.
	 *
	 * <p>
	 * 专门用于处理字符串数据, 避免不必要的序列化开销.
	 * @param redisConnectionFactory Redis 连接工厂.
	 * @return 一个专门用于处理字符串的 StringRedisTemplate 实例.
	 */
	@Bean
	@ConditionalOnMissingBean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}

}
