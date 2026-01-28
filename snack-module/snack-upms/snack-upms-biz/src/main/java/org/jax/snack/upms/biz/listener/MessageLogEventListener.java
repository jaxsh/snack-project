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

package org.jax.snack.upms.biz.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.message.event.MessageLogEvent;
import org.jax.snack.upms.biz.entity.SysMessageLog;
import org.jax.snack.upms.biz.repository.SysMessageLogRepository;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 消息日志事件监听器.
 * <p>
 * 监听消息发送事件并保存日志到数据库.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageLogEventListener {

	private final SysMessageLogRepository repository;

	/**
	 * 处理消息日志事件.
	 * @param event 消息日志事件
	 */
	@Async
	@EventListener
	public void onMessageLog(MessageLogEvent event) {
		SysMessageLog entity = new SysMessageLog();
		entity.setTemplateCode(event.getTemplateCode());
		entity.setChannelType(event.getChannelType());
		entity.setChannelId(event.getChannelId());
		entity.setRecipient(event.getRecipient());
		entity.setTitle(event.getTitle());
		entity.setContent(event.getContent());
		entity.setParams(event.getParams());
		entity.setStatus(event.getStatus());
		entity.setErrorMsg(event.getErrorMsg());
		entity.setChannelResponse(event.getChannelResponse());
		this.repository.save(entity);
		log.debug("Message log saved: templateCode={}, recipient={}, status={}", event.getTemplateCode(),
				event.getRecipient(), event.getStatus());
	}

}
