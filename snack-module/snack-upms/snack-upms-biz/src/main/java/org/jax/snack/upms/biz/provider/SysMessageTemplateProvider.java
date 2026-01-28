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

package org.jax.snack.upms.biz.provider;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.message.core.MessageTemplate;
import org.jax.snack.framework.message.core.TemplateProvider;
import org.jax.snack.upms.biz.entity.SysMessageTemplate;
import org.jax.snack.upms.biz.repository.SysMessageTemplateRepository;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 消息模版提供者实现.
 * <p>
 * 从 sys_message_template 表加载模版数据.
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class SysMessageTemplateProvider implements TemplateProvider {

	private final SysMessageTemplateRepository repository;

	@Override
	public MessageTemplate getTemplate(String templateCode, String channelType) {
		QueryCondition condition = QueryCondition.builder()
			.eq(SysMessageTemplate.Fields.templateCode, templateCode)
			.eq(SysMessageTemplate.Fields.templateType, channelType)
			.build();
		List<SysMessageTemplate> list = this.repository.queryListByDsl(condition);
		if (CollectionUtils.isEmpty(list)) {
			return null;
		}
		SysMessageTemplate entity = list.get(0);
		if (!Status.ENABLED.getCode().equals(entity.getStatus())) {
			return null;
		}
		return new MessageTemplate(entity.getTemplateCode(), entity.getTemplateType(), entity.getTitle(),
				entity.getContent(), entity.getTemplateConfig());
	}

}
