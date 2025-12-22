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

package org.jax.snack.framework.utils.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 树形结构构建工具类, 提供将扁平数据转换为树形结构的功能.
 *
 * @author Jax Jiang
 */
public final class TreeBuilder {

	private TreeBuilder() {
	}

	/**
	 * 构建树形结构.
	 * <p>
	 * 如果不指定节点ID, 则构建所有根节点的完整树; 如果指定节点ID, 则只构建这些节点的子树.
	 * @param <T> 数据类型
	 * @param <ID> ID 类型
	 * @param items 待构建的数据列表
	 * @param rootValue 根节点的父ID值 (例如 null 或 0)
	 * @param idExtractor 提取节点 ID 的函数
	 * @param parentIdExtractor 提取父节点 ID 的函数
	 * @param nodeIds 可选: 指定要构建的节点ID, 不传则构建完整树
	 * @return 树形结构的根节点列表
	 */
	@SafeVarargs
	public static <T, ID> List<TreeNode<T>> build(List<T> items, ID rootValue, Function<T, ID> idExtractor,
			Function<T, ID> parentIdExtractor, ID... nodeIds) {
		Objects.requireNonNull(items, "Items must not be null");
		Objects.requireNonNull(idExtractor, "ID extractor must not be null");
		Objects.requireNonNull(parentIdExtractor, "Parent ID extractor must not be null");

		if (nodeIds == null || nodeIds.length == 0) {
			return buildFullTree(items, idExtractor, parentIdExtractor, rootValue);
		}

		Set<ID> startNodeSet = new HashSet<>(Arrays.asList(nodeIds));
		if (startNodeSet.contains(rootValue)) {
			return buildFullTree(items, idExtractor, parentIdExtractor, rootValue);
		}

		Map<ID, T> itemMap = new HashMap<>();
		Map<ID, List<ID>> childrenMap = new HashMap<>();

		for (T item : items) {
			ID id = idExtractor.apply(item);
			ID parentId = parentIdExtractor.apply(item);

			itemMap.put(id, item);
			childrenMap.computeIfAbsent(parentId, (k) -> new ArrayList<>()).add(id);
		}

		Set<ID> validStartNodes = filterTopLevelNodes(startNodeSet, itemMap, parentIdExtractor, rootValue);

		List<TreeNode<T>> result = new ArrayList<>();
		for (ID nodeId : validStartNodes) {
			result.add(buildTreeRecursively(nodeId, itemMap, childrenMap));
		}

		return result;
	}

	private static <T, ID> List<TreeNode<T>> buildFullTree(List<T> items, Function<T, ID> idExtractor,
			Function<T, ID> parentIdExtractor, ID rootValue) {
		Map<ID, TreeNode<T>> nodeMap = new HashMap<>();
		List<TreeNode<T>> roots = new ArrayList<>();

		for (T item : items) {
			ID id = idExtractor.apply(item);
			nodeMap.put(id, new TreeNode<>(item));
		}

		for (T item : items) {
			ID id = idExtractor.apply(item);
			ID parentId = parentIdExtractor.apply(item);
			TreeNode<T> currentNode = nodeMap.get(id);

			if (isRoot(parentId, rootValue)) {
				roots.add(currentNode);
			}
			else {
				TreeNode<T> parentNode = nodeMap.get(parentId);
				if (parentNode != null) {
					parentNode.addChild(currentNode);
				}
				else {
					roots.add(currentNode);
				}
			}
		}

		return roots;
	}

	private static <T, ID> Set<ID> filterTopLevelNodes(Set<ID> candidateNodes, Map<ID, T> itemMap,
			Function<T, ID> parentIdExtractor, ID rootValue) {
		Set<ID> topLevelNodes = new HashSet<>();

		for (ID candidateId : candidateNodes) {
			boolean hasAncestorInCandidates = false;
			ID currentId = candidateId;

			while (currentId != null) {
				T currentItem = itemMap.get(currentId);
				if (currentItem == null) {
					break;
				}

				ID parentId = parentIdExtractor.apply(currentItem);

				if (isRoot(parentId, rootValue)) {
					break;
				}

				if (candidateNodes.contains(parentId)) {
					hasAncestorInCandidates = true;
					break;
				}

				currentId = parentId;
			}

			if (!hasAncestorInCandidates) {
				topLevelNodes.add(candidateId);
			}
		}

		return topLevelNodes;
	}

	private static <T, ID> TreeNode<T> buildTreeRecursively(ID nodeId, Map<ID, T> itemMap,
			Map<ID, List<ID>> childrenMap) {
		T item = itemMap.get(nodeId);
		if (item == null) {
			return null;
		}

		TreeNode<T> node = new TreeNode<>(item);
		List<ID> childIds = childrenMap.get(nodeId);

		if (childIds != null) {
			childIds.forEach((childId) -> node.addChild(buildTreeRecursively(childId, itemMap, childrenMap)));
		}

		return node;
	}

	/**
	 * 查找指定节点的所有后代节点 ID (包含自身).
	 * @param <T> 数据类型
	 * @param <ID> ID 类型
	 * @param roots 树的根节点列表
	 * @param idExtractor 提取节点 ID 的函数
	 * @param targetNodeIds 目标节点 ID 数组
	 * @return 所有后代节点的 ID 集合
	 */
	@SafeVarargs
	public static <T, ID> Set<ID> findDescendants(List<TreeNode<T>> roots, Function<T, ID> idExtractor,
			ID... targetNodeIds) {
		Set<ID> targetSet = new HashSet<>(Arrays.asList(targetNodeIds));
		Set<ID> result = new HashSet<>();

		for (TreeNode<T> root : roots) {
			collectDescendantsForQuery(root, idExtractor, targetSet, result, false);
		}

		return result;
	}

	private static <T, ID> void collectDescendantsForQuery(TreeNode<T> node, Function<T, ID> idExtractor,
			Set<ID> targetNodes, Set<ID> result, boolean collecting) {
		ID nodeId = idExtractor.apply(node.getData());
		boolean isTarget = targetNodes.contains(nodeId);

		if (collecting || isTarget) {
			result.add(nodeId);
			collecting = true;
		}

		if (node.hasChildren()) {
			for (TreeNode<T> child : node.getChildren()) {
				collectDescendantsForQuery(child, idExtractor, targetNodes, result, collecting);
			}
		}
	}

	private static <ID> boolean isRoot(ID parentId, ID rootValue) {
		return Objects.equals(rootValue, parentId);
	}

}
