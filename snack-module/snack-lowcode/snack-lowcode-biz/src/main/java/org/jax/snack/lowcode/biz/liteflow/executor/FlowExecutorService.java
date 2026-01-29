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

package org.jax.snack.lowcode.biz.liteflow.executor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.lowcode.biz.entity.LowcodeBusinessFlow;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowChain;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowStep;
import org.jax.snack.lowcode.biz.liteflow.chain.ChainRefreshService;
import org.jax.snack.lowcode.biz.liteflow.context.BusinessFlowContext;
import org.jax.snack.lowcode.biz.repository.LowcodeBusinessFlowRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowChainRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowStepRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 流程执行服务.
 * <p>
 * 执行业务流程, 管理事务边界.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowExecutorService {

	private final FlowExecutor flowExecutor;

	private final ChainRefreshService chainRefreshService;

	private final LowcodeBusinessFlowRepository businessFlowRepository;

	private final LowcodeFlowChainRepository flowChainRepository;

	private final LowcodeFlowStepRepository flowStepRepository;

	private final ObjectMapper objectMapper;

	/**
	 * 执行业务流程.
	 * @param flowCode 流程编码
	 * @param data 业务数据
	 * @return 执行结果
	 */
	@Transactional(rollbackFor = Exception.class)
	public Object execute(String flowCode, Map<String, Object> data) {
		return execute(flowCode, data, null);
	}

	/**
	 * 执行业务流程 (带 ID).
	 * @param flowCode 流程编码
	 * @param data 业务数据
	 * @param id 主键 ID
	 * @return 执行结果
	 */
	@Transactional(rollbackFor = Exception.class)
	public Object execute(String flowCode, Map<String, Object> data, Long id) {
		LowcodeBusinessFlow flow = this.businessFlowRepository.findByFlowCode(flowCode)
			.orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowCode));

		if (!Objects.equals(flow.getEnabled(), Status.ENABLED.getCode())) {
			throw new IllegalStateException("Flow is disabled: " + flowCode);
		}

		LowcodeFlowChain chain = this.flowChainRepository.findByFlowId(flow.getId())
			.orElseThrow(() -> new IllegalArgumentException("Chain not found for flow: " + flowCode));

		String chainId = this.chainRefreshService.buildChainId(flowCode);

		if (!this.chainRefreshService.exists(flowCode)) {
			this.chainRefreshService.refresh(flowCode);
		}

		BusinessFlowContext context = buildContext(flow, chain, data, id);

		log.info("Executing flow: {}, chainId: {}", flowCode, chainId);
		LiteflowResponse response = this.flowExecutor.execute2Resp(chainId, context);

		if (!response.isSuccess()) {
			log.error("Flow execution failed: {}", flowCode, response.getCause());
			throw new RuntimeException("Flow execution failed: " + response.getMessage(), response.getCause());
		}

		log.info("Flow executed successfully: {}", flowCode);
		return context.getResult();
	}

	private BusinessFlowContext buildContext(LowcodeBusinessFlow flow, LowcodeFlowChain chain, Map<String, Object> data,
			Long id) {
		BusinessFlowContext context = new BusinessFlowContext();
		context.setFlowCode(flow.getFlowCode());
		context.setMainSchema(flow.getMainSchema());
		context.setData(data);
		context.setId(id);

		List<LowcodeFlowStep> steps = this.flowStepRepository.findByChainId(chain.getId());
		for (LowcodeFlowStep step : steps) {
			if (Objects.equals(step.getEnabled(), Status.ENABLED.getCode()) && step.getConfigJson() != null) {
				parseStepConfig(context, step);
			}
		}

		return context;
	}

	@SneakyThrows
	private void parseStepConfig(BusinessFlowContext context, LowcodeFlowStep step) {
		@SuppressWarnings("unchecked")
		Map<String, Object> config = this.objectMapper.readValue(step.getConfigJson(), Map.class);
		context.setStepConfig(step.getStepId(), config);
	}

}
