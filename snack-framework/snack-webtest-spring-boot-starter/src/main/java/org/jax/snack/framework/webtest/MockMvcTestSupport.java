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

package org.jax.snack.framework.webtest;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * MockMvc 全链路测试基类.
 * <p>
 * 提供预配置的 JSON 请求方法和统一的 JsonMapper, 简化 Web 层集成测试.
 * <p>
 * 所有请求自动注入 Mock JWT Token, 兼容 OAuth2 Resource Server 安全配置.
 *
 * @author Jax Jiang
 */
public abstract class MockMvcTestSupport {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JsonMapper jsonMapper;

	/**
	 * 执行 GET 请求并设置 JSON 内容类型.
	 * @param url 请求路径
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions getJson(String url) throws Exception {
		return this.mockMvc.perform(get(url).with(defaultJwt()).contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 执行 GET 请求并应用 RequestPostProcessor (用于注入 SecurityContext 等).
	 * @param url 请求路径
	 * @param postProcessors 请求后置处理器
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions getJson(String url, RequestPostProcessor... postProcessors) throws Exception {
		MockHttpServletRequestBuilder requestBuilder = get(url).with(defaultJwt())
			.contentType(MediaType.APPLICATION_JSON);
		for (RequestPostProcessor postProcessor : postProcessors) {
			requestBuilder.with(postProcessor);
		}
		return this.mockMvc.perform(requestBuilder);
	}

	/**
	 * 执行带路径参数的 GET 请求.
	 * @param urlTemplate 路径模板
	 * @param uriVariables 路径参数
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions getJson(String urlTemplate, Object... uriVariables) throws Exception {
		return this.mockMvc
			.perform(get(urlTemplate, uriVariables).with(defaultJwt()).contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 执行 POST 请求, 请求体序列化为 JSON.
	 * @param url 请求路径
	 * @param body 请求体对象
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions postJson(String url, Object body) throws Exception {
		return this.mockMvc.perform(post(url).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.jsonMapper.writeValueAsString(body)));
	}

	/**
	 * 执行不带请求体的 POST 请求.
	 * @param url 请求路径
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions postJson(String url) throws Exception {
		return this.mockMvc.perform(post(url).with(defaultJwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 执行带路径参数但不带请求体的 POST 请求.
	 * @param urlTemplate 路径模板
	 * @param uriVariables 路径参数
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions postJsonNoBody(String urlTemplate, Object... uriVariables) throws Exception {
		return this.mockMvc.perform(post(urlTemplate, uriVariables).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 执行带路径参数的 POST 请求.
	 * @param urlTemplate 路径模板
	 * @param body 请求体对象
	 * @param uriVariables 路径参数
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions postJson(String urlTemplate, Object body, Object... uriVariables) throws Exception {
		return this.mockMvc.perform(post(urlTemplate, uriVariables).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.jsonMapper.writeValueAsString(body)));
	}

	/**
	 * 执行 PUT 请求, 请求体序列化为 JSON.
	 * @param url 请求路径
	 * @param body 请求体对象
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions putJson(String url, Object body) throws Exception {
		return this.mockMvc.perform(put(url).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.jsonMapper.writeValueAsString(body)));
	}

	/**
	 * 执行带路径参数的 PUT 请求.
	 * @param urlTemplate 路径模板
	 * @param body 请求体对象
	 * @param uriVariables 路径参数
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions putJson(String urlTemplate, Object body, Object... uriVariables) throws Exception {
		return this.mockMvc.perform(put(urlTemplate, uriVariables).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content(this.jsonMapper.writeValueAsString(body)));
	}

	/**
	 * 执行 DELETE 请求.
	 * @param url 请求路径
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions deleteJson(String url) throws Exception {
		return this.mockMvc
			.perform(delete(url).with(defaultJwt()).with(csrf()).contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 执行带路径参数的 DELETE 请求.
	 * @param urlTemplate 路径模板
	 * @param uriVariables 路径参数
	 * @return ResultActions
	 * @throws Exception 请求异常
	 */
	protected ResultActions deleteJson(String urlTemplate, Object... uriVariables) throws Exception {
		return this.mockMvc.perform(delete(urlTemplate, uriVariables).with(defaultJwt())
			.with(csrf())
			.contentType(MediaType.APPLICATION_JSON));
	}

	/**
	 * 获取默认的 JWT RequestPostProcessor (携带 test:permission 权限标识).
	 * <p>
	 * 直接注入 Authority 规避 OAuth2 Scope 逻辑对测试的干扰.
	 * @return JwtRequestPostProcessor
	 */
	protected JwtRequestPostProcessor defaultJwt() {
		return jwt().authorities(new SimpleGrantedAuthority("test:permission"));
	}

	/**
	 * 将对象序列化为 JSON 字符串.
	 * @param object 要序列化的对象
	 * @return JSON 字符串
	 */
	protected String toJson(Object object) {
		return this.jsonMapper.writeValueAsString(object);
	}

}
