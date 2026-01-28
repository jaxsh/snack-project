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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.enums.SuccessStatus;
import org.jax.snack.framework.message.MessageProperties;
import org.jax.snack.framework.message.event.MessageLogEvent;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 默认消息发送器实现.
 *
 * @author Jax Jiang
 */
@Slf4j
public class DefaultMessageSender implements MessageSender {

	private final TemplateProvider templateProvider;

	private final TemplateRenderer templateRenderer;

	private final Map<String, Map<String, MessageChannel>> channelRegistry;

	private final Executor executor;

	private final ApplicationEventPublisher eventPublisher;

	private final MessageProperties properties;

	/**
	 * 构造函数.
	 * @param templateProvider 模版提供者 (可选)
	 * @param templateRenderer 模版渲染器
	 * @param channels 渠道列表
	 * @param executor 异步执行器
	 * @param eventPublisher 事件发布器
	 * @param properties 配置属性
	 */
	public DefaultMessageSender(TemplateProvider templateProvider, TemplateRenderer templateRenderer,
			List<MessageChannel> channels, Executor executor, ApplicationEventPublisher eventPublisher,
			MessageProperties properties) {
		this.templateProvider = templateProvider;
		this.templateRenderer = templateRenderer;
		this.executor = executor;
		this.eventPublisher = eventPublisher;
		this.properties = properties;

		this.channelRegistry = new HashMap<>();
		for (MessageChannel channel : channels) {
			this.channelRegistry.computeIfAbsent(channel.getType(), (k) -> new HashMap<>())
				.put(channel.getId(), channel);
		}
	}

	@Override
	public CompletableFuture<MessageResult> send(MessageRequest request) {
		return CompletableFuture.supplyAsync(() -> doSend(request), this.executor);
	}

	@Override
	public List<CompletableFuture<MessageResult>> sendBatch(List<MessageRequest> requests) {
		List<CompletableFuture<MessageResult>> futures = new ArrayList<>();
		for (MessageRequest request : requests) {
			futures.add(send(request));
		}
		return futures;
	}

	private MessageResult doSend(MessageRequest request) {
		String templateCode = request.getTemplateCode();
		String channelType = request.getChannelType();
		List<String> recipients = request.getRecipients();

		MessageTemplate template = null;
		if (!request.isSkipTemplate() && StringUtils.hasText(templateCode) && this.templateProvider != null) {
			template = this.templateProvider.getTemplate(templateCode, channelType);
			if (template == null) {
				String errorMsg = "Template not found or disabled: " + templateCode;
				log.warn(errorMsg);
				publishLogEvent(request, null, null, null, SuccessStatus.FAIL.getCode(), errorMsg, null);
				return MessageResult.ofFail(errorMsg);
			}
		}

		String title = StringUtils.hasText(request.getTitle()) ? request.getTitle()
				: ((template != null) ? template.title() : "");
		String content = StringUtils.hasText(request.getContent()) ? request.getContent()
				: ((template != null) ? template.content() : "");

		title = this.templateRenderer.render(title, request.getParams());
		content = this.templateRenderer.render(content, request.getParams());

		Map<String, Object> config = (template != null) ? template.config() : null;
		String channelId = resolveChannelId(request, config);

		MessageChannel channel = getChannel(channelType, channelId);
		if (channel == null) {
			String errorMsg = "Channel not found: type=" + channelType + ", id=" + channelId;
			log.warn(errorMsg);
			publishLogEvent(request, channelId, title, content, SuccessStatus.FAIL.getCode(), errorMsg, null);
			return MessageResult.ofFail(errorMsg);
		}

		if (!channel.isEnabled()) {
			String errorMsg = "Channel is disabled: type=" + channelType + ", id=" + channelId;
			log.warn(errorMsg);
			publishLogEvent(request, channelId, title, content, SuccessStatus.FAIL.getCode(), errorMsg, null);
			return MessageResult.ofFail(errorMsg);
		}

		MessageDTO messageDto = MessageDTO.builder()
			.to(recipients)
			.cc(request.getCc())
			.bcc(request.getBcc())
			.templateCode(templateCode)
			.params(request.getParams())
			.title(title)
			.content(content)
			.attachments(request.getAttachments())
			.extras(config)
			.build();

		MessageResult result = channel.send(messageDto);
		Integer status = result.isSuccess() ? SuccessStatus.SUCCESS.getCode() : SuccessStatus.FAIL.getCode();
		publishLogEvent(request, channelId, title, content, status, result.getErrorMsg(), result.getRawResponse());
		return result;
	}

	private String resolveChannelId(MessageRequest request, Map<String, Object> templateConfig) {
		if (StringUtils.hasText(request.getChannelId())) {
			return request.getChannelId();
		}
		if (!CollectionUtils.isEmpty(templateConfig) && templateConfig.containsKey("channelId")) {
			return (String) templateConfig.get("channelId");
		}
		if ("SMS".equals(request.getChannelType()) && this.properties.getSms() != null) {
			return this.properties.getSms().getDefaultChannel();
		}
		return "default";
	}

	private MessageChannel getChannel(String type, String id) {
		Map<String, MessageChannel> channels = this.channelRegistry.get(type);
		if (channels == null) {
			return null;
		}
		MessageChannel channel = channels.get(id);
		if (channel == null) {
			channel = channels.get("default");
		}
		return channel;
	}

	private void publishLogEvent(MessageRequest request, String channelId, String title, String content, Integer status,
			String errorMsg, Map<String, Object> rawResponse) {
		List<String> recipients = request.getRecipients();
		if (CollectionUtils.isEmpty(recipients)) {
			return;
		}
		for (String recipient : recipients) {
			MessageLogEvent event = MessageLogEvent.builder()
				.templateCode(request.getTemplateCode())
				.channelType(request.getChannelType())
				.channelId(channelId)
				.recipient(recipient)
				.title(title)
				.content(content)
				.params(request.getParams())
				.status(status)
				.errorMsg(errorMsg)
				.channelResponse(rawResponse)
				.build();
			this.eventPublisher.publishEvent(event);
		}
	}

}
