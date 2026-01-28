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

package org.jax.snack.framework.message.core;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 消息发送接口.
 *
 * @author Jax Jiang
 */
public interface MessageSender {

	/**
	 * 异步发送消息.
	 * @param request 发送请求
	 * @return 发送结果 Future
	 */
	CompletableFuture<MessageResult> send(MessageRequest request);

	/**
	 * 批量发送消息 (多渠道).
	 * @param requests 发送请求列表
	 * @return 发送结果 Future 列表
	 */
	List<CompletableFuture<MessageResult>> sendBatch(List<MessageRequest> requests);

}
