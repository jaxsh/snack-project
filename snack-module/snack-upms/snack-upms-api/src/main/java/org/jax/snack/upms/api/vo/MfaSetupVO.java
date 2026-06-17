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

package org.jax.snack.upms.api.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MFA 初始化响应.
 *
 * @author Jax Jiang
 */
@Getter
@AllArgsConstructor
public class MfaSetupVO {

	/**
	 * Base32 编码的 TOTP 共享密钥.
	 */
	private final String secret;

	/**
	 * otpauth:// URI（用于渲染二维码）.
	 */
	private final String otpauthUri;

}
