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
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.message.event.SiteMessageEvent;
import org.jax.snack.upms.biz.entity.SysNotification;
import org.jax.snack.upms.biz.repository.SysNotificationRepository;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 站内信事件监听器.
 * <p>
 * 监听站内信事件并保存到数据库.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteMessageEventListener {

	private final SysNotificationRepository repository;

	/**
	 * 处理站内信事件.
	 * @param event 站内信事件
	 */
	@Async
	@EventListener
	public void onSiteMessage(SiteMessageEvent event) {
		for (String username : event.getRecipients()) {
			SysNotification entity = new SysNotification();
			entity.setUsername(username);
			entity.setTitle(event.getTitle());
			entity.setContent(event.getContent());
			entity.setReadFlag(YesNoStatus.NO.getCode());
			this.repository.save(entity);
		}
		log.debug("Site notification saved for {} recipients", event.getRecipients().size());
	}

}
