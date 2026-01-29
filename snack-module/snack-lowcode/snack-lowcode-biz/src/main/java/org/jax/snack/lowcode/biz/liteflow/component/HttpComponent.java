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

package org.jax.snack.lowcode.biz.liteflow.component;

import java.util.Locale;
import java.util.Map;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

/**
 * HTTP 调用组件.
 * <p>
 * 支持调用外部 HTTP 接口, 并将结果存入上下文变量.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("httpCmp")
@RequiredArgsConstructor
public class HttpComponent extends NodeComponent {

	private static final String CONFIG_URL = "url";

	private static final String CONFIG_METHOD = "method";

	private static final String CONFIG_RESULT_VARIABLE = "resultVariable";

	private static final String CONFIG_EXECUTE_MODE = "executeMode";

	private static final String EXECUTE_MODE_ASYNC = "ASYNC";

	private final RestClient.Builder restClientBuilder;

	@Override
	public void process() {
		BusinessFlowContext context = getContextBean(BusinessFlowContext.class);
		String stepId = getNodeId();
		Map<String, Object> config = context.getStepConfig(stepId);

		String url = (String) config.get(CONFIG_URL);
		String method = (String) config.getOrDefault(CONFIG_METHOD, "POST");
		String resultVariable = (String) config.getOrDefault(CONFIG_RESULT_VARIABLE, "httpResponse");
		String executeMode = (String) config.getOrDefault(CONFIG_EXECUTE_MODE, "SYNC");

		url = replacePlaceholders(url, context.getData());

		if (EXECUTE_MODE_ASYNC.equalsIgnoreCase(executeMode)) {
			registerAsyncExecution(url, method, context.getData());
		}
		else {
			Object response = executeRequest(url, method, context.getData());
			context.setVariable(resultVariable, response);
		}
	}

	private Object executeRequest(String url, String method, Map<String, Object> data) {
		log.debug("Executing HTTP {} to: {}", method, url);

		RestClient restClient = this.restClientBuilder.build();
		return restClient.method(HttpMethod.valueOf(method.toUpperCase(Locale.ROOT)))
			.uri(url)
			.body(data)
			.retrieve()
			.body(Object.class);
	}

	private void registerAsyncExecution(String url, String method, Map<String, Object> data) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					log.debug("Executing HTTP {} asynchronously to: {}", method, url);
					executeRequest(url, method, data);
				}
			});
		}
		else {
			executeRequest(url, method, data);
		}
	}

	private String replacePlaceholders(String url, Map<String, Object> data) {
		if (url == null) {
			return null;
		}
		String result = url;
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String placeholder = "${" + entry.getKey() + "}";
			if (result.contains(placeholder) && entry.getValue() != null) {
				result = result.replace(placeholder, entry.getValue().toString());
			}
		}
		return result;
	}

}
