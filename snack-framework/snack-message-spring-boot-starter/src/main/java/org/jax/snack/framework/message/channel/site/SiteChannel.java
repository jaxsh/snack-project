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

package org.jax.snack.framework.message.channel.site;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.message.core.MessageChannel;
import org.jax.snack.framework.message.core.MessageConstants;
import org.jax.snack.framework.message.core.MessageDTO;
import org.jax.snack.framework.message.core.MessageResult;
import org.jax.snack.framework.message.event.SiteMessageEvent;

import org.springframework.context.ApplicationEventPublisher;

/**
 * 站内信渠道实现.
 * <p>
 * 通过发布事件实现解耦, 由业务系统监听处理.
 *
 * @author Jax Jiang
 */
@Slf4j
@RequiredArgsConstructor
public class SiteChannel implements MessageChannel {

	private final ApplicationEventPublisher eventPublisher;

	@Override
	public String getType() {
		return MessageConstants.CHANNEL_TYPE_SITE;
	}

	@Override
	public String getId() {
		return "default";
	}

	@Override
	public MessageResult send(MessageDTO message) {
		this.eventPublisher.publishEvent(SiteMessageEvent.builder()
			.recipients(message.getTo())
			.title(message.getTitle())
			.content(message.getContent())
			.build());
		log.debug("Site message event published for recipients: {}", message.getTo());
		return MessageResult.ofSuccess();
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
