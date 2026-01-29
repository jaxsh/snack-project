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

package org.jax.snack.lowcode.biz.liteflow.component;

import java.util.List;
import java.util.Map;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.message.core.MessageRequest;
import org.jax.snack.framework.message.core.MessageSender;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 消息发送组件.
 * <p>
 * 统一消息发送 (Mail/SMS/Site), 通过配置区分渠道. 支持同步/异步执行模式.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("messageCmp")
@RequiredArgsConstructor
public class MessageComponent extends NodeComponent {

	private static final String CONFIG_CHANNEL_TYPE = "channelType";

	private static final String CONFIG_TEMPLATE_CODE = "templateCode";

	private static final String CONFIG_RECIPIENT_FIELD = "recipientField";

	private static final String CONFIG_EXECUTE_MODE = "executeMode";

	private static final String EXECUTE_MODE_ASYNC = "ASYNC";

	private final MessageSender messageSender;

	@Override
	public void process() {
		BusinessFlowContext context = getContextBean(BusinessFlowContext.class);
		String stepId = getNodeId();
		Map<String, Object> config = context.getStepConfig(stepId);

		String channelType = (String) config.getOrDefault(CONFIG_CHANNEL_TYPE, "MAIL");
		String templateCode = (String) config.get(CONFIG_TEMPLATE_CODE);
		String recipientField = (String) config.getOrDefault(CONFIG_RECIPIENT_FIELD, "email");
		String executeMode = (String) config.getOrDefault(CONFIG_EXECUTE_MODE, "SYNC");

		String recipient = resolveRecipient(context.getData(), recipientField);
		if (recipient == null) {
			log.warn("Recipient not found in field: {}", recipientField);
			return;
		}

		MessageRequest request = MessageRequest.builder()
			.channelType(channelType)
			.recipients(List.of(recipient))
			.templateCode(templateCode)
			.params(context.getData())
			.build();

		if (EXECUTE_MODE_ASYNC.equalsIgnoreCase(executeMode)) {
			registerAsyncExecution(request);
		}
		else {
			sendSync(request);
		}
	}

	private String resolveRecipient(Map<String, Object> data, String field) {
		Object value = data.get(field);
		return (value != null) ? value.toString() : null;
	}

	private void sendSync(MessageRequest request) {
		log.debug("Sending message synchronously: {}", request);
		this.messageSender.send(request).join();
	}

	private void registerAsyncExecution(MessageRequest request) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					log.debug("Sending message asynchronously after commit: {}", request);
					MessageComponent.this.messageSender.send(request);
				}
			});
		}
		else {
			this.messageSender.send(request);
		}
	}

}
