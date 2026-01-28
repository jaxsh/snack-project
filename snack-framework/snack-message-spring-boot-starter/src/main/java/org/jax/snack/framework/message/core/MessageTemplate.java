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

package org.jax.snack.framework.message.core;

import java.util.Map;

/**
 * 消息模版 DTO.
 *
 * @param templateCode 模版编码
 * @param templateType 模版类型
 * @param title 标题
 * @param content 内容
 * @param config 渠道特定配置
 * @author Jax Jiang
 */
public record MessageTemplate(String templateCode, String templateType, String title, String content,
		Map<String, Object> config) {

}
