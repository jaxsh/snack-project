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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis 自动配置类.
 *
 * <p>
 * 该配置旨在提供一个即开即用的、经过优化的 {@link RedisTemplate} Bean. 它统一了序列化策略, 使用 JSON 格式存储对象, 增强了 Redis
 * 数据的可读性和跨语言兼容性.
 *
 * @author Jax Jiang
 * @since 2025-05-30
 */
@AutoConfiguration
public class RedisAutoConfiguration {

	/**
	 * 创建并配置一个 {@link RedisTemplate} Bean.
	 *
	 * <p>
	 * 核心设计决策如下:
	 * <ul>
	 * <li>键 (Key) 序列化: 使用 {@link RedisSerializer#string()}. 字符串类型的键具有最佳的可读性, 并且便于在各种工具 (如
	 * redis-cli) 中进行调试.</li>
	 * <li>值 (Value) 序列化: 使用 {@link GenericJackson2JsonRedisSerializer}. 与
	 * {@code Jackson2JsonRedisSerializer} 不同, 此序列化器会在 JSON 中嵌入完整的 Java 类型信息,
	 * 从而解决了反序列化时无法还原为具体对象类型 (导致 {@code ClassCastException}) 的常见问题.</li>
	 * <li>依赖注入 {@code ObjectMapper}: 重用 Spring Boot 自动配置的 {@code ObjectMapper} 实例, 确保
	 * Redis 中的 JSON 序列化行为 (如日期格式, 模块注册等) 与应用其他部分 (如 Web API) 保持一致.</li>
	 * </ul>
	 * @param redisConnectionFactory 由 Spring Boot 自动提供的 Redis 连接工厂.
	 * @param objectMapper 由 Spring Boot 自动配置并注入的 Jackson ObjectMapper 实例.
	 * @return 一个完全配置好、可以立即注入使用的 RedisTemplate 实例.
	 */
	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory,
			ObjectMapper objectMapper) {
		// 创建一个支持泛型和类型信息的JSON序列化器.
		// 这是确保反序列化能够正确还原为原始对象类型的关键.
		RedisSerializer<Object> jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);

		// 为 Key 和 HashKey 设置字符串序列化器.
		redisTemplate.setKeySerializer(RedisSerializer.string());
		redisTemplate.setHashKeySerializer(RedisSerializer.string());

		// 为 Value 和 HashValue 设置通用的JSON序列化器.
		redisTemplate.setValueSerializer(jsonSerializer);
		redisTemplate.setHashValueSerializer(jsonSerializer);

		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

	/**
	 * 创建并配置一个 {@link StringRedisTemplate} Bean.
	 *
	 * <p>
	 * <b>用途与优势:</b>
	 * <ul>
	 * <li>{@code StringRedisTemplate} 是 {@code RedisTemplate} 的一个特化版本, 专门用于处理字符串数据.</li>
	 * <li>当您确认键和值都是字符串时 (例如: 存储计数器、Session ID、简单的标志位等), 使用它会更高效、更方便.</li>
	 * <li>它避免了不必要的对象序列化/反序列化开销, 直接使用字符串进行操作.</li>
	 * </ul>
	 * <p>
	 * 此 Bean 与上面的 {@code redisTemplate} Bean 共存, 开发者可以根据具体场景选择注入哪一个.
	 * @param redisConnectionFactory 由 Spring Boot 自动提供的 Redis 连接工厂.
	 * @return 一个专门用于处理字符串的 StringRedisTemplate 实例.
	 */
	@Bean
	@ConditionalOnMissingBean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		return new StringRedisTemplate(redisConnectionFactory);
	}

}
