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

package org.jax.snack.lowcode.biz.service.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.lowcode.api.dto.FlowDesignDTO;
import org.jax.snack.lowcode.api.dto.FlowDesignDTO.FlowEdgeDTO;
import org.jax.snack.lowcode.api.dto.FlowDesignDTO.FlowNodeDTO;
import org.jax.snack.lowcode.biz.entity.LowcodeBusinessFlow;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowChain;
import org.jax.snack.lowcode.biz.entity.LowcodeFlowStep;
import org.jax.snack.lowcode.biz.liteflow.chain.ChainRefreshService;
import org.jax.snack.lowcode.biz.repository.LowcodeBusinessFlowRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowChainRepository;
import org.jax.snack.lowcode.biz.repository.LowcodeFlowStepRepository;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 流程设计器服务.
 * <p>
 * 处理可视化设计器的保存和加载.
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDesignerService {

	private static final int PARALLEL_THRESHOLD = 1;

	private final LowcodeBusinessFlowRepository businessFlowRepository;

	private final LowcodeFlowChainRepository flowChainRepository;

	private final LowcodeFlowStepRepository flowStepRepository;

	private final ChainRefreshService chainRefreshService;

	private final ObjectMapper objectMapper;

	/**
	 * 保存流程设计.
	 * @param design 设计 DTO
	 */
	@Transactional(rollbackFor = Exception.class)
	public void saveDesign(FlowDesignDTO design) {
		LowcodeBusinessFlow flow = saveBusinessFlow(design);
		LowcodeFlowChain chain = saveFlowChain(flow, design);
		saveFlowSteps(chain, design);
		this.chainRefreshService.refresh(flow.getFlowCode());
		log.info("Flow design saved: {}", flow.getFlowCode());
	}

	/**
	 * 获取流程设计.
	 * @param flowCode 流程编码
	 * @return 设计 DTO
	 */
	public FlowDesignDTO getDesign(String flowCode) {
		LowcodeBusinessFlow flow = this.businessFlowRepository.findByFlowCode(flowCode).orElse(null);
		if (flow == null) {
			return null;
		}

		FlowDesignDTO design = new FlowDesignDTO();
		design.setFlowCode(flow.getFlowCode());
		design.setFlowName(flow.getFlowName());
		design.setDescription(flow.getDescription());
		design.setMainSchema(flow.getMainSchema());
		design.setTriggerType(flow.getTriggerType());

		this.flowChainRepository.findByFlowId(flow.getId())
			.ifPresent((LowcodeFlowChain chain) -> design.setDesignJson(chain.getChainJson()));

		return design;
	}

	private LowcodeBusinessFlow saveBusinessFlow(FlowDesignDTO design) {
		LowcodeBusinessFlow flow = this.businessFlowRepository.findByFlowCode(design.getFlowCode())
			.orElseGet(LowcodeBusinessFlow::new);

		flow.setFlowCode(design.getFlowCode());
		flow.setFlowName(design.getFlowName());
		flow.setDescription(design.getDescription());
		flow.setMainSchema(design.getMainSchema());
		flow.setTriggerType(StringUtils.hasText(design.getTriggerType()) ? design.getTriggerType() : "API");
		flow.setEnabled(Status.ENABLED.getCode());

		if (flow.getId() == null) {
			flow.setVersion(1);
			this.businessFlowRepository.save(flow);
		}
		else {
			flow.setVersion(flow.getVersion() + 1);
			this.businessFlowRepository.update(flow);
		}

		return flow;
	}

	private LowcodeFlowChain saveFlowChain(LowcodeBusinessFlow flow, FlowDesignDTO design) {
		LowcodeFlowChain chain = this.flowChainRepository.findByFlowId(flow.getId()).orElseGet(LowcodeFlowChain::new);

		chain.setFlowId(flow.getId());
		chain.setChainEl(convertToChainEl(design));
		chain.setChainJson(design.getDesignJson());
		chain.setExecuteMode("SYNC");

		if (chain.getId() == null) {
			chain.setVersion(1);
			this.flowChainRepository.save(chain);
		}
		else {
			chain.setVersion(chain.getVersion() + 1);
			this.flowChainRepository.update(chain);
		}

		return chain;
	}

	private void saveFlowSteps(LowcodeFlowChain chain, FlowDesignDTO design) {
		this.flowStepRepository.deleteByChainId(chain.getId());

		if (CollectionUtils.isEmpty(design.getNodes())) {
			return;
		}

		int sortOrder = 0;
		for (FlowNodeDTO node : design.getNodes()) {
			LowcodeFlowStep step = new LowcodeFlowStep();
			step.setChainId(chain.getId());
			step.setStepId(node.getId());
			step.setComponentType(node.getType());
			step.setStepName(node.getName());
			step.setConfigJson(serializeConfig(node.getConfig()));
			step.setExecuteMode(StringUtils.hasText(node.getExecuteMode()) ? node.getExecuteMode() : "SYNC");
			step.setSortOrder(sortOrder);
			step.setEnabled(Status.ENABLED.getCode());
			this.flowStepRepository.save(step);
			sortOrder++;
		}
	}

	private String convertToChainEl(FlowDesignDTO design) {
		if (CollectionUtils.isEmpty(design.getNodes())) {
			return "THEN()";
		}

		List<FlowNodeDTO> nodes = design.getNodes();
		List<FlowEdgeDTO> edges = design.getEdges();

		if (CollectionUtils.isEmpty(edges)) {
			return buildSequentialEl(nodes);
		}

		return buildGraphEl(nodes, edges);
	}

	private String buildSequentialEl(List<FlowNodeDTO> nodes) {
		String components = nodes.stream().map(this::nodeToComponent).collect(Collectors.joining(", "));
		return "THEN(" + components + ")";
	}

	private String buildGraphEl(List<FlowNodeDTO> nodes, List<FlowEdgeDTO> edges) {
		Map<String, FlowNodeDTO> nodeMap = nodes.stream()
			.collect(Collectors.toMap(FlowNodeDTO::getId, (FlowNodeDTO n) -> n));

		Map<String, List<String>> outgoing = new HashMap<>();
		Set<String> hasIncoming = new HashSet<>();

		for (FlowEdgeDTO edge : edges) {
			outgoing.computeIfAbsent(edge.getSource(), (String k) -> new ArrayList<>()).add(edge.getTarget());
			hasIncoming.add(edge.getTarget());
		}

		List<String> roots = nodes.stream()
			.map(FlowNodeDTO::getId)
			.filter((String id) -> !hasIncoming.contains(id))
			.toList();

		if (roots.isEmpty()) {
			return buildSequentialEl(nodes);
		}

		List<String> elParts = new ArrayList<>();
		Set<String> visited = new HashSet<>();

		for (String root : roots) {
			buildElRecursive(root, nodeMap, outgoing, elParts, visited);
		}

		return "THEN(" + String.join(", ", elParts) + ")";
	}

	private void buildElRecursive(String nodeId, Map<String, FlowNodeDTO> nodeMap, Map<String, List<String>> outgoing,
			List<String> elParts, Set<String> visited) {
		if (visited.contains(nodeId)) {
			return;
		}
		visited.add(nodeId);

		FlowNodeDTO node = nodeMap.get(nodeId);
		if (node == null) {
			return;
		}

		List<String> children = outgoing.getOrDefault(nodeId, List.of());

		if ("ITERATOR".equalsIgnoreCase(node.getType()) && !children.isEmpty()) {
			String iteratorCmp = nodeToComponent(node);
			List<String> childEls = new ArrayList<>();
			for (String childId : children) {
				FlowNodeDTO childNode = nodeMap.get(childId);
				if (childNode != null) {
					childEls.add(nodeToComponent(childNode));
					visited.add(childId);
				}
			}
			elParts.add(String.format("ITERATOR(%s).DO(%s)", iteratorCmp, String.join(", ", childEls)));
		}
		else {
			elParts.add(nodeToComponent(node));
			if (children.size() > PARALLEL_THRESHOLD) {
				List<String> parallelParts = new ArrayList<>();
				for (String childId : children) {
					parallelParts.add(nodeToComponent(nodeMap.get(childId)));
					visited.add(childId);
				}
				elParts.add("WHEN(" + String.join(", ", parallelParts) + ")");
			}
			else {
				for (String childId : children) {
					buildElRecursive(childId, nodeMap, outgoing, elParts, visited);
				}
			}
		}
	}

	private String nodeToComponent(FlowNodeDTO node) {
		String componentId = switch (node.getType().toUpperCase(Locale.ROOT)) {
			case "CRUD" -> "crudCmp";
			case "MESSAGE" -> "messageCmp";
			case "HTTP" -> "httpCmp";
			case "ITERATOR" -> "iteratorCmp";
			case "VALIDATE" -> "validateCmp";
			default -> node.getType().toLowerCase(Locale.ROOT) + "Cmp";
		};
		return componentId + ".tag(\"" + node.getId() + "\")";
	}

	@SneakyThrows
	private String serializeConfig(Map<String, Object> config) {
		if (config == null) {
			return null;
		}
		return this.objectMapper.writeValueAsString(config);
	}

}
