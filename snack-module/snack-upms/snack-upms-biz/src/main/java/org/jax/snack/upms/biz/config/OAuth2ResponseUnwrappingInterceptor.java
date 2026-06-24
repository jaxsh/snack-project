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

package org.jax.snack.upms.biz.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.http.exception.InterfaceBusinessException;
import org.jax.snack.framework.http.exception.InterfaceException;
import org.jax.snack.framework.http.handler.ErrorWrappingInterceptor;
import org.jax.snack.framework.web.model.ApiResponse;
import org.jspecify.annotations.NullMarked;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

/**
 * OAuth2 客户端响应解包和错误处理拦截器.
 *
 * @author Jax Jiang
 */
@NullMarked
@RequiredArgsConstructor
public class OAuth2ResponseUnwrappingInterceptor implements ErrorWrappingInterceptor {

	private final JsonMapper jsonMapper;

	/**
	 * 拦截HTTP请求并处理响应, 对包含 ApiResponse 结构的响应进行解包或提取异常信息.
	 * @param request http请求
	 * @param body 请求体
	 * @param execution 请求执行器
	 * @return 处理后的 http 响应
	 * @throws IOException 当发生 I/O 错误时抛出
	 */
	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		ClientHttpResponse response = execution.execute(request, body);
		try {
			MediaType contentType = response.getHeaders().getContentType();
			if (contentType != null && contentType.includes(MediaType.APPLICATION_JSON)) {
				byte[] responseBytes = response.getBody().readAllBytes();
				if (responseBytes.length > 0) {
					String content = new String(responseBytes, StandardCharsets.UTF_8);
					ApiResponse<?> apiResponse;
					try {
						apiResponse = this.jsonMapper.readValue(content, ApiResponse.class);
					}
					catch (JacksonException ex) {
						return new BufferedClientHttpResponse(response, responseBytes);
					}

					if (apiResponse != null) {
						if (!apiResponse.isSuccess()) {
							throw new InterfaceBusinessException(apiResponse.getMsg());
						}

						Object data = apiResponse.getData();
						byte[] dataBytes;
						if (data != null) {
							dataBytes = this.jsonMapper.writeValueAsBytes(data);
						}
						else {
							dataBytes = new byte[0];
						}

						return new UnwrappedClientHttpResponse(response, dataBytes);
					}
				}
			}
			return response;
		}
		catch (IOException ex) {
			throw new InterfaceException(ErrorCode.INTERFACE_ERROR, ex);
		}
	}

	/**
	 * 解包响应包装类.
	 */
	private static class UnwrappedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;

		private final byte[] bodyBytes;

		UnwrappedClientHttpResponse(ClientHttpResponse response, byte[] bodyBytes) {
			this.response = response;
			this.bodyBytes = bodyBytes;
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return this.response.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.response.getStatusText();
		}

		@Override
		public void close() {
			this.response.close();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.bodyBytes);
		}

		@Override
		public HttpHeaders getHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.putAll(this.response.getHeaders());
			headers.setContentLength(this.bodyBytes.length);
			return headers;
		}

	}

	/**
	 * 带缓存的原始响应类.
	 */
	private static class BufferedClientHttpResponse implements ClientHttpResponse {

		private final ClientHttpResponse response;

		private final byte[] bodyBytes;

		BufferedClientHttpResponse(ClientHttpResponse response, byte[] bodyBytes) {
			this.response = response;
			this.bodyBytes = bodyBytes;
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return this.response.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.response.getStatusText();
		}

		@Override
		public void close() {
			this.response.close();
		}

		@Override
		public InputStream getBody() {
			return new ByteArrayInputStream(this.bodyBytes);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.response.getHeaders();
		}

	}

}
