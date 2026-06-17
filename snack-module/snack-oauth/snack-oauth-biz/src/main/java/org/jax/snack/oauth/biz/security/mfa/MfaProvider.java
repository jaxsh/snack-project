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

package org.jax.snack.oauth.biz.security.mfa;

/**
 * MFA 验证渠道 SPI.
 * <p>
 * TOTP：{@link #send} 为空操作；{@link #verify} 读本地 DB 完成时间算法验证.
 *
 * @author Jax Jiang
 */
public interface MfaProvider {

	/**
	 * 发送 MFA 挑战. TOTP 为空操作；SMS/邮件：生成码 → Spring Cache → 通知投递.
	 * @param username 用户名
	 */
	void send(String username);

	/**
	 * 校验用户提交的验证码.
	 * @param username 用户名
	 * @param code 6 位验证码
	 * @return 校验通过返回 {@code true}
	 */
	boolean verify(String username, String code);

}
