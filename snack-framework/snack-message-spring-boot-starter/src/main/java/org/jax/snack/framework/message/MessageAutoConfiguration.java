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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.jax.snack.framework.message.channel.mail.MailChannel;
import org.jax.snack.framework.message.channel.site.SiteChannel;
import org.jax.snack.framework.message.channel.sms.AliyunSmsChannel;
import org.jax.snack.framework.message.core.DefaultMessageSender;
import org.jax.snack.framework.message.core.MessageChannel;
import org.jax.snack.framework.message.core.MessageSender;
import org.jax.snack.framework.message.core.TemplateProvider;
import org.jax.snack.framework.message.core.TemplateRenderer;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.CollectionUtils;

/**
 * 消息发送自动配置.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
@AutoConfigureAfter(MailSenderAutoConfiguration.class)
public class MessageAutoConfiguration {

	/**
	 * 消息发送执行器.
	 * @param properties 配置属性
	 * @return 执行器
	 */
	@Bean
	@ConditionalOnMissingBean(name = "messageSenderExecutor")
	public Executor messageSenderExecutor(MessageProperties properties) {
		MessageProperties.ExecutorProperties executorProps = properties.getAsync();
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(executorProps.getCorePoolSize());
		executor.setMaxPoolSize(executorProps.getMaxPoolSize());
		executor.setQueueCapacity(executorProps.getQueueCapacity());
		executor.setThreadNamePrefix(executorProps.getThreadNamePrefix());
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.initialize();
		return executor;
	}

	/**
	 * 模版渲染器.
	 * @return 渲染器
	 */
	@Bean
	@ConditionalOnMissingBean
	public TemplateRenderer templateRenderer() {
		return new TemplateRenderer();
	}

	/**
	 * 站内信渠道.
	 * @param eventPublisher 事件发布器
	 * @return 站内信渠道
	 */
	@Bean
	@ConditionalOnMissingBean(SiteChannel.class)
	public SiteChannel siteChannel(ApplicationEventPublisher eventPublisher) {
		return new SiteChannel(eventPublisher);
	}

	/**
	 * 邮件渠道.
	 * @param mailSender 邮件发送器
	 * @param from 发件人
	 * @return 邮件渠道
	 */
	@Bean
	@ConditionalOnBean(JavaMailSender.class)
	@ConditionalOnMissingBean(MailChannel.class)
	public MailChannel mailChannel(JavaMailSender mailSender, @Value("${spring.mail.username:}") String from) {
		return new MailChannel(mailSender, from);
	}

	/**
	 * 消息发送器.
	 * @param templateProvider 模版提供者 (可选)
	 * @param templateRenderer 模版渲染器
	 * @param channels 渠道列表
	 * @param messageSenderExecutor 执行器
	 * @param eventPublisher 事件发布器
	 * @param properties 配置属性
	 * @param jsonMapperProvider JSON Mapper 提供者
	 * @return 消息发送器
	 */
	@Bean
	@ConditionalOnMissingBean
	public MessageSender messageSender(ObjectProvider<TemplateProvider> templateProvider,
			TemplateRenderer templateRenderer, List<MessageChannel> channels, Executor messageSenderExecutor,
			ApplicationEventPublisher eventPublisher, MessageProperties properties,
			ObjectProvider<JsonMapper> jsonMapperProvider) {

		List<MessageChannel> allChannels = new ArrayList<>(channels);

		Map<String, Map<String, String>> blends = properties.getSms().getBlends();
		JsonMapper jsonMapper = jsonMapperProvider.getIfAvailable();

		if (jsonMapper != null && !CollectionUtils.isEmpty(blends)) {
			blends.forEach((id, config) -> {
				if (config.containsKey("accessKeyId")) {
					allChannels.add(new AliyunSmsChannel(id, config, jsonMapper));
				}
			});
		}

		return new DefaultMessageSender(templateProvider.getIfAvailable(), templateRenderer, allChannels,
				messageSenderExecutor, eventPublisher, properties);
	}

}
