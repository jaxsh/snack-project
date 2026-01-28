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

package org.jax.snack.framework.message.config;

import java.util.Map;

import org.jax.snack.framework.message.MessageAutoConfiguration;
import org.jax.snack.framework.message.MessageProperties;
import org.jax.snack.framework.message.channel.sms.AliyunSmsChannel;
import org.jax.snack.framework.message.channel.sms.SmsProperties;
import org.jax.snack.framework.message.core.DefaultMessageSender;
import org.jax.snack.framework.message.core.MessageChannel;
import org.jax.snack.framework.message.core.MessageConstants;
import org.jax.snack.framework.message.core.MessageSender;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SMS 配置验证测试.
 *
 * @author Jax Jiang
 */
class SmsConfigurationTests {

	private static final String ALIYUN_TEST = "aliyun-test";

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MessageAutoConfiguration.class));

	@Test
	void shouldLoadAliyunSmsChannelFromBlends() {
		this.runner
			.withPropertyValues("snack.message.sms.default-channel=" + ALIYUN_TEST,
					"snack.message.sms.blends." + ALIYUN_TEST + ".accessKeyId=fakeId",
					"snack.message.sms.blends." + ALIYUN_TEST + ".accessKeySecret=fakeSecret",
					"snack.message.sms.blends." + ALIYUN_TEST + ".signName=MySign")
			.withUserConfiguration(JsonMapperConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(MessageProperties.class);
				MessageProperties properties = context.getBean(MessageProperties.class);
				SmsProperties smsProperties = properties.getSms();
				Map<String, Map<String, String>> blends = smsProperties.getBlends();
				assertThat(blends).containsKey(ALIYUN_TEST);
				assertThat(blends.get(ALIYUN_TEST)).containsEntry("accessKeyId", "fakeId");

				assertThat(context).hasSingleBean(MessageSender.class);
				MessageSender sender = context.getBean(MessageSender.class);
				assertThat(sender).isInstanceOf(DefaultMessageSender.class);

				@SuppressWarnings("unchecked")
				Map<String, Map<String, MessageChannel>> registry = (Map<String, Map<String, MessageChannel>>) getFieldValue(
						sender);

				assertThat(registry).containsKey(MessageConstants.CHANNEL_TYPE_SMS);
				Map<String, MessageChannel> smsChannels = registry.get(MessageConstants.CHANNEL_TYPE_SMS);
				assertThat(smsChannels).containsKey(ALIYUN_TEST);
				assertThat(smsChannels.get(ALIYUN_TEST)).isInstanceOf(AliyunSmsChannel.class);
			});
	}

	private Object getFieldValue(Object target) {
		return ReflectionTestUtils.getField(target, "channelRegistry");
	}

	@Configuration
	static class JsonMapperConfig {

		@Bean
		JsonMapper jsonMapper() {
			return JsonMapper.builder().build();
		}

	}

}
