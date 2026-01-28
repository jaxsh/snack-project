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

package org.jax.snack.upms.message;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.framework.core.enums.SuccessStatus;
import org.jax.snack.framework.core.enums.YesNoStatus;
import org.jax.snack.framework.message.core.MessageConstants;
import org.jax.snack.framework.message.core.MessageRequest;
import org.jax.snack.framework.message.core.MessageResult;
import org.jax.snack.framework.message.core.MessageSender;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.biz.entity.SysMessageLog;
import org.jax.snack.upms.biz.entity.SysMessageTemplate;
import org.jax.snack.upms.biz.entity.SysNotification;
import org.jax.snack.upms.biz.repository.SysMessageLogRepository;
import org.jax.snack.upms.biz.repository.SysMessageTemplateRepository;
import org.jax.snack.upms.biz.repository.SysNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * 消息发送集成测试.
 *
 * @author Jax Jiang
 */
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = { "spring.mail.username=test@example.com" })
@Import(MessageSenderTests.MailSenderConfiguration.class)
class MessageSenderTests extends UpmsIntegrationTests {

	@Autowired
	private MessageSender messageSender;

	@Autowired
	private SysMessageTemplateRepository templateRepository;

	@Autowired
	private SysNotificationRepository notificationRepository;

	@Autowired
	private SysMessageLogRepository messageLogRepository;

	@Autowired
	private JavaMailSender javaMailSender;

	private static final String TEMPLATE_SITE_MSG = "TEST_SITE_MSG";

	@BeforeEach
	void prepareData() {
		this.notificationRepository.deleteByDsl(WhereCondition.builder().isNotNull(SysNotification.Fields.id).build());
		this.messageLogRepository.deleteByDsl(WhereCondition.builder().isNotNull(SysMessageLog.Fields.id).build());
		this.templateRepository.deleteByDsl(WhereCondition.builder().isNotNull(SysMessageTemplate.Fields.id).build());
	}

	/**
	 * 站内信发送测试.
	 */
	@Nested
	class SiteMessage {

		@Test
		void shouldSendSiteMessageWithTemplate() throws Exception {
			SysMessageTemplate template = new SysMessageTemplate();
			template.setTemplateCode(TEMPLATE_SITE_MSG);
			template.setTemplateName("测试站内信");
			template.setTemplateType(MessageConstants.CHANNEL_TYPE_SITE);
			template.setTitle("订单 ${orderNo} 已完成");
			template.setContent("您的订单 ${orderNo} 已完成, 金额 ${amount} 元");
			template.setStatus(Status.ENABLED.getCode());
			MessageSenderTests.this.templateRepository.save(template);

			Map<String, Object> params = Map.of("orderNo", "ORD202601220001", "amount", "99.00");
			String userId = "1001";

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.templateCode(TEMPLATE_SITE_MSG)
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(List.of(userId))
					.params(params)
					.build());

			MessageResult result = future.get(5, TimeUnit.SECONDS);

			assertThat(result.isSuccess()).isTrue();

			Thread.sleep(500);

			List<SysNotification> notifications = MessageSenderTests.this.notificationRepository.queryListByDsl(null);
			assertThat(notifications).isNotEmpty();

			SysNotification notification = notifications.stream()
				.filter((n) -> n.getUsername().equals(userId))
				.findFirst()
				.orElse(null);
			assertThat(notification).isNotNull();
			assertThat(notification.getTitle()).isEqualTo("订单 ORD202601220001 已完成");
			assertThat(notification.getContent()).contains("金额 99.00 元");
			assertThat(notification.getReadFlag()).isEqualTo(YesNoStatus.NO.getCode());
		}

		@Test
		void shouldSendSiteMessageDirectly() throws Exception {
			String userId = "2001";
			String title = "系统公告";
			String content = "系统将于今晚 22:00 进行维护";

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(List.of(userId))
					.title(title)
					.content(content)
					.build());

			MessageResult result = future.get(5, TimeUnit.SECONDS);

			assertThat(result.isSuccess()).isTrue();

			Thread.sleep(500);

			List<SysNotification> notifications = MessageSenderTests.this.notificationRepository.queryListByDsl(null);
			SysNotification notification = notifications.stream()
				.filter((n) -> n.getUsername().equals(userId))
				.findFirst()
				.orElse(null);
			assertThat(notification).isNotNull();
			assertThat(notification.getTitle()).isEqualTo(title);
			assertThat(notification.getContent()).isEqualTo(content);
		}

		@Test
		void shouldSendToMultipleRecipients() throws Exception {
			List<String> userIds = List.of("3001", "3002", "3003");

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(userIds)
					.title("批量通知")
					.content("这是一条批量通知")
					.build());

			MessageResult result = future.get(5, TimeUnit.SECONDS);
			assertThat(result.isSuccess()).isTrue();

			Thread.sleep(500);

			List<SysNotification> notifications = MessageSenderTests.this.notificationRepository.queryListByDsl(null);
			for (String userId : userIds) {
				boolean hasNotification = notifications.stream().anyMatch((n) -> n.getUsername().equals(userId));
				assertThat(hasNotification).as("User %s should receive notification", userId).isTrue();
			}
		}

	}

	@Nested
	class MessageLog {

		@Test
		@DisplayName("发送消息后应记录日志")
		void shouldLogMessageAfterSend() throws Exception {
			String userId = "4001";

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(List.of(userId))
					.title("日志测试")
					.content("测试消息日志记录")
					.build());

			future.get(5, TimeUnit.SECONDS);

			Thread.sleep(500);

			List<SysMessageLog> logs = MessageSenderTests.this.messageLogRepository.queryListByDsl(null);
			SysMessageLog log = logs.stream().filter((l) -> userId.equals(l.getRecipient())).findFirst().orElse(null);
			assertThat(log).isNotNull();
			assertThat(log.getChannelType()).isEqualTo(MessageConstants.CHANNEL_TYPE_SITE);
			assertThat(log.getStatus()).isEqualTo(SuccessStatus.SUCCESS.getCode());
			assertThat(log.getTitle()).isEqualTo("日志测试");
		}

	}

	@Nested
	class MailMessage {

		@Test
		void shouldSendMailWithCcBccAndAttachments() throws Exception {
			MimeMessage mimeMessage = new MimeMessage((Session) null);
			given(MessageSenderTests.this.javaMailSender.createMimeMessage()).willReturn(mimeMessage);

			String to = "user@example.com";
			String cc = "cc@example.com";
			String bcc = "bcc@example.com";
			String title = "邮件测试";
			String content = "<h1>这是一封测试邮件</h1>";

			Path tempFile = Files.createTempFile("test-attachment", ".txt");
			Files.writeString(tempFile, "这是附件内容");
			File attachment = tempFile.toFile();
			attachment.deleteOnExit();

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.channelType(MessageConstants.CHANNEL_TYPE_MAIL)
					.recipients(List.of(to))
					.cc(List.of(cc))
					.bcc(List.of(bcc))
					.attachments(List.of(attachment))
					.title(title)
					.content(content)
					.build());

			MessageResult result = future.get(10, TimeUnit.SECONDS);

			assertThat(result.isSuccess()).isTrue();
		}

	}

	@Nested
	class ErrorScenarios {

		@Test
		void shouldFailWhenTemplateNotFound() throws Exception {
			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.templateCode("NON_EXISTENT_TEMPLATE")
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(List.of("5001"))
					.build());

			MessageResult result = future.get(5, TimeUnit.SECONDS);

			assertThat(result.isSuccess()).isFalse();
			assertThat(result.getErrorMsg()).contains("Template not found");
		}

		@Test
		void shouldFailWhenTemplateDisabled() throws Exception {
			SysMessageTemplate template = new SysMessageTemplate();
			template.setTemplateCode("DISABLED_TEMPLATE");
			template.setTemplateName("已停用模版");
			template.setTemplateType(MessageConstants.CHANNEL_TYPE_SITE);
			template.setTitle("测试");
			template.setContent("测试内容");
			template.setStatus(Status.DISABLED.getCode());
			MessageSenderTests.this.templateRepository.save(template);

			CompletableFuture<MessageResult> future = MessageSenderTests.this.messageSender
				.send(MessageRequest.builder()
					.templateCode("DISABLED_TEMPLATE")
					.channelType(MessageConstants.CHANNEL_TYPE_SITE)
					.recipients(List.of("5002"))
					.build());

			MessageResult result = future.get(5, TimeUnit.SECONDS);

			assertThat(result.isSuccess()).isFalse();
			assertThat(result.getErrorMsg()).contains("Template not found");
		}

	}

	@TestConfiguration
	static class MailSenderConfiguration {

		@Bean
		JavaMailSender javaMailSender() {
			return Mockito.mock(JavaMailSender.class);
		}

	}

}
