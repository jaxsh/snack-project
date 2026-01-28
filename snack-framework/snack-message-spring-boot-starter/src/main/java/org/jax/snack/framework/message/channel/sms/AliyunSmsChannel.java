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

package org.jax.snack.framework.message.channel.sms;

import java.util.Map;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.message.core.MessageDTO;
import org.jax.snack.framework.message.core.MessageResult;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 阿里云短信渠道实现.
 *
 * @author Jax Jiang
 */
@Slf4j
public class AliyunSmsChannel implements SmsChannel {

	private final String id;

	private final Client client;

	private final String signName;

	private final JsonMapper jsonMapper;

	private boolean enabled = true;

	/**
	 * 构造函数.
	 * @param id 渠道标识
	 * @param config 配置参数
	 * @param jsonMapper JSON Mapper
	 */
	@SneakyThrows
	public AliyunSmsChannel(String id, Map<String, String> config, JsonMapper jsonMapper) {
		this.id = id;
		this.jsonMapper = jsonMapper;
		if (config.containsKey("enabled")) {
			this.enabled = Boolean.parseBoolean(config.get("enabled"));
		}
		this.signName = config.get("signName");

		Config aliyunConfig = new Config().setAccessKeyId(config.get("accessKeyId"))
			.setAccessKeySecret(config.get("accessKeySecret"));

		aliyunConfig.endpoint = config.getOrDefault("endpoint", "dysmsapi.aliyuncs.com");

		this.client = new Client(aliyunConfig);
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public MessageResult send(MessageDTO message) {
		try {
			String phoneNumber = String.join(",", message.getTo());
			String templateParam = this.jsonMapper.writeValueAsString(message.getParams());

			Map<String, Object> extras = message.getExtras();

			String currentTemplateCode = message.getTemplateCode();
			if (extras != null && extras.containsKey("templateCode")) {
				currentTemplateCode = (String) extras.get("templateCode");
			}

			String currentSignName = this.signName;
			if (extras != null && extras.containsKey("signName")) {
				currentSignName = (String) extras.get("signName");
			}

			SendSmsRequest request = new SendSmsRequest().setPhoneNumbers(phoneNumber)
				.setSignName(currentSignName)
				.setTemplateCode(currentTemplateCode)
				.setTemplateParam(templateParam);

			RuntimeOptions runtime = new RuntimeOptions();
			SendSmsResponse response = this.sendSms(request, runtime);

			if (!"OK".equalsIgnoreCase(response.getBody().getCode())) {
				String errorMsg = String.format("Aliyun SMS failed: code=%s, message=%s", response.getBody().getCode(),
						response.getBody().getMessage());
				log.error(errorMsg);
				return MessageResult.ofFail(errorMsg);
			}

			@SuppressWarnings("unchecked")
			Map<String, Object> rawResponse = this.jsonMapper.convertValue(response.getBody(), Map.class);
			return MessageResult.ofSuccess(rawResponse);
		}
		catch (TeaException | JacksonException ex) {
			log.error("Failed to send Aliyun SMS", ex);
			return MessageResult.ofFail(ex.getMessage());
		}
	}

	@SneakyThrows(Exception.class)
	private SendSmsResponse sendSms(SendSmsRequest request, RuntimeOptions runtime) {
		return this.client.sendSmsWithOptions(request, runtime);
	}

}
