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

/**
 * 模版提供者 SPI 接口.
 * <p>
 * 业务模块实现此接口以提供模版数据.
 *
 * @author Jax Jiang
 */
public interface TemplateProvider {

	/**
	 * 根据模版编码和渠道类型获取模版.
	 * @param templateCode 模版编码
	 * @param channelType 渠道类型
	 * @return 模版信息, 不存在返回 null
	 */
	MessageTemplate getTemplate(String templateCode, String channelType);

}
