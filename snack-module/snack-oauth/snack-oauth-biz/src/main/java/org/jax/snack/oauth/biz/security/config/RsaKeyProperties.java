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

package org.jax.snack.oauth.biz.security.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RSA 密钥配置属性.
 * <p>
 * 支持多密钥配置, 用于实现密钥轮换.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "snack.oauth.security.rsa")
public class RsaKeyProperties {

	/**
	 * 密钥列表.
	 */
	private List<KeyEntry> keys = new ArrayList<>();

	/**
	 * 获取当前活跃的密钥.
	 * @return 活跃密钥, 如果没有则返回列表中第一个
	 */
	public KeyEntry getActiveKey() {
		return this.keys.stream()
			.filter(KeyEntry::isActive)
			.findFirst()
			.orElseGet(() -> this.keys.isEmpty() ? null : this.keys.get(0));
	}

	/**
	 * 密钥条目.
	 */
	@Getter
	@Setter
	public static class KeyEntry {

		/**
		 * 密钥 ID.
		 */
		private String keyId;

		/**
		 * 公钥.
		 */
		private RSAPublicKey publicKey;

		/**
		 * 私钥 (仅活跃密钥需要).
		 */
		private RSAPrivateKey privateKey;

		/**
		 * 是否为当前签发密钥.
		 */
		private boolean active;

	}

}
