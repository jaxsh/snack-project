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

package org.jax.snack.lowcode.biz.liteflow.chain;

import java.util.List;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.FlowBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.lowcode.biz.entity.LowcodeBusinessFlow;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowChain;
import org.jax.snack.lowcode.biz.repository.LowcodeBusinessFlowRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowChainRepository;

import org.springframework.stereotype.Service;

/**
 * Chain 刷新服务.
 * <p>
 * 从数据库加载 Chain 定义并注册到 LiteFlow.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainRefreshService {

	private final LowcodeBusinessFlowRepository businessFlowRepository;

	private final LowcodeFlowChainRepository flowChainRepository;

	/**
	 * 刷新指定流程的 Chain.
	 * @param flowCode 流程编码
	 */
	public void refresh(String flowCode) {
		this.businessFlowRepository.findByFlowCode(flowCode).flatMap(flow -> this.flowChainRepository.findByFlowId(flow.getId())).ifPresent((LowcodeFlowChain chain) -> {
			String chainId = buildChainId(flowCode);
			registerChain(chainId, chain.getChainEl());
			log.info("Refreshed chain: {}", chainId);
		});
	}

	/**
	 * 刷新所有启用的 Chain.
	 */
	public void refreshAll() {
		List<LowcodeBusinessFlow> flows = this.businessFlowRepository.findAllEnabled();

		for (LowcodeBusinessFlow flow : flows) {
			this.flowChainRepository.findByFlowId(flow.getId()).ifPresent((LowcodeFlowChain chain) -> {
				String chainId = buildChainId(flow.getFlowCode());
				registerChain(chainId, chain.getChainEl());
			});
		}
		log.info("Refreshed {} chains from database", flows.size());
	}

	/**
	 * 检查 Chain 是否存在.
	 * @param flowCode 流程编码
	 * @return 是否存在
	 */
	public boolean exists(String flowCode) {
		String chainId = buildChainId(flowCode);
		return FlowBus.getChain(chainId) != null;
	}

	/**
	 * 构建 Chain ID.
	 * @param flowCode 流程编码
	 * @return Chain ID
	 */
	public String buildChainId(String flowCode) {
		return "flow_" + flowCode;
	}

	private void registerChain(String chainId, String chainEl) {
		try {
			LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(chainEl).build();
		}
		catch (ELParseException ex) {
			log.error("Failed to register chain: {}, el: {}", chainId, chainEl, ex);
		}
	}

}
