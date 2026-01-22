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

package org.jax.snack.framework.oauth2.client.spi;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * OAuth2 Token 操作客户端接口.
 * <p>
 * 使用声明式 HTTP 客户端调用 Authorization Server 的 Token 端点.
 *
 * @author Jax Jiang
 */
@HttpExchange
public interface OAuth2TokenClient {

	/**
	 * 吊销 Token.
	 * <p>
	 * 调用 Authorization Server 的 /oauth2/revoke 端点.
	 * @param formData 表单数据 (token, token_type_hint)
	 */
	@PostExchange(url = "/oauth2/revoke", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	void revokeToken(@RequestBody MultiValueMap<String, String> formData);

}
