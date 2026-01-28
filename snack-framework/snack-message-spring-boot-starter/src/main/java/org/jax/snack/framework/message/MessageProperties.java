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

package org.jax.snack.framework.message;

import lombok.Getter;
import lombok.Setter;
import org.jax.snack.framework.message.channel.sms.SmsProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息发送配置属性.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "snack.message")
public class MessageProperties {

	/**
	 * 异步执行器.
	 */
	private ExecutorProperties async = new ExecutorProperties();

	/**
	 * 短信.
	 */
	private SmsProperties sms = new SmsProperties();

	/**
	 * 异步执行器.
	 */
	@Getter
	@Setter
	public static class ExecutorProperties {

		/**
		 * 核心线程数.
		 */
		private int corePoolSize = 10;

		/**
		 * 最大线程数.
		 */
		private int maxPoolSize = 30;

		/**
		 * 队列容量.
		 */
		private int queueCapacity = 100;

		/**
		 * 线程名前缀.
		 */
		private String threadNamePrefix = "msg-sender-";

	}

}
