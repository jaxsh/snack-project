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

package org.jax.snack.framework.message.channel.mail;

import java.io.File;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.message.core.MessageChannel;
import org.jax.snack.framework.message.core.MessageConstants;
import org.jax.snack.framework.message.core.MessageDTO;
import org.jax.snack.framework.message.core.MessageResult;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.CollectionUtils;

/**
 * 邮件渠道实现.
 * <p>
 * 支持 HTML 富文本.
 *
 * @author Jax Jiang
 */
@Slf4j
@RequiredArgsConstructor
public class MailChannel implements MessageChannel {

	private final JavaMailSender mailSender;

	private final String from;

	@Override
	public String getType() {
		return MessageConstants.CHANNEL_TYPE_MAIL;
	}

	@Override
	public String getId() {
		return "default";
	}

	@Override
	public MessageResult send(MessageDTO message) {
		try {
			MimeMessage mimeMessage = this.mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
			helper.setFrom(this.from);
			helper.setTo(message.getTo().toArray(new String[0]));

			if (!CollectionUtils.isEmpty(message.getCc())) {
				helper.setCc(message.getCc().toArray(new String[0]));
			}
			if (!CollectionUtils.isEmpty(message.getBcc())) {
				helper.setBcc(message.getBcc().toArray(new String[0]));
			}

			helper.setSubject(message.getTitle());
			helper.setText(message.getContent(), true);

			if (!CollectionUtils.isEmpty(message.getAttachments())) {
				for (File file : message.getAttachments()) {
					helper.addAttachment(file.getName(), file);
				}
			}

			this.mailSender.send(mimeMessage);
			log.info("Mail sent successfully to {}", message.getTo());
			return MessageResult.ofSuccess();
		}
		catch (MessagingException ex) {
			log.error("Failed to send mail to {}: {}", message.getTo(), ex.getMessage());
			return MessageResult.ofFail(ex.getMessage());
		}
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

}
