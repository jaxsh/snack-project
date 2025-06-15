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

package org.jax.snack.framework.mdc;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.core.pattern.DynamicConverter;
import ch.qos.logback.core.pattern.FormatInfo;
import ch.qos.logback.core.pattern.parser.CompositeNode;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.SimpleKeywordNode;

/**
 * 用于程序化操作 Logback 日志格式（Pattern）内部节点树的工具类. 此类提供底层能力，以可靠的方式检查和修改已解析的日志格式.
 *
 * @author Jax Jiang
 * @since 2025-06-02
 */
public final class LogbackNodeUtils {

	/**
	 * 缓存 Converter 类与其对应的格式关键字（如 "level", "p"）之间的映射. 键由 PatternLayout 实例的
	 * identityHashCode 和 Converter 类名构成，以确保缓存的正确性. 用于避免在每次检查时重复计算，提高性能.
	 */
	private static final ConcurrentMap<String, Set<String>> KEYWORD_CACHE = new ConcurrentHashMap<>();

	private LogbackNodeUtils() {
	}

	/**
	 * 在节点树中进行深度优先搜索，查找第一个满足条件的节点. 此方法会递归进入复合节点（CompositeNode）进行搜索.
	 * @param node 搜索的起始节点.
	 * @param predicate 用于测试每个节点的断言.
	 * @return 第一个匹配的节点；如果未找到，则返回 null.
	 */
	public static Node findNode(Node node, Predicate<Node> predicate) {
		while (node != null) {
			if (predicate.test(node)) {
				return node;
			}
			if (node instanceof CompositeNode) {
				Node foundInChild = findNode(((CompositeNode) node).getChildNode(), predicate);
				if (foundInChild != null) {
					return foundInChild;
				}
			}
			node = node.getNext();
		}
		return null;
	}

	/**
	 * 在给定的节点树中，向目标节点后注入一个新的节点链.
	 * @param topNode 要操作的节点树的起始节点.
	 * @param injectHead 要注入的新节点链的头节点.
	 * @param targetNodePredicate 用于识别注入位置的断言.
	 * @return 如果成功注入，则返回 true；否则返回 false.
	 */
	public static boolean recursiveInject(Node topNode, Node injectHead, Predicate<Node> targetNodePredicate) {
		Node injectionTarget = findInjectionTargetNode(topNode, targetNodePredicate);

		if (injectionTarget == null) {
			return false;
		}

		Node insertionPointPrev = injectionTarget;
		Node walker = injectionTarget.getNext();

		// 跳过紧跟在目标后面的任何字面量节点（如空格），以确保注入位置正确.
		while (walker != null) {
			if (walker.getType() != 0) { // 0 代表字面量 (literal)
				break;
			}
			insertionPointPrev = walker;
			walker = walker.getNext();
		}

		Node originalNext = insertionPointPrev.getNext();
		Node injectTail = findTail(injectHead);
		insertionPointPrev.setNext(injectHead);
		if (injectTail != null) {
			injectTail.setNext(originalNext);
		}
		return true;
	}

	/**
	 * 根据指定的 Converter 类，动态地创建一个用于识别相关节点的断言（Predicate）.
	 * @param targetConverterClass 目标 Converter 的 Class 对象.
	 * @param layout 当前的 PatternLayout 实例，用于访问其转换器映射.
	 * @return 一个可以判断节点是否与目标 Converter 关联的 Predicate.
	 */
	@SuppressWarnings("rawtypes")
	public static Predicate<Node> getNodePredicateForConverter(Class<?> targetConverterClass, PatternLayout layout) {
		String cacheKey = System.identityHashCode(layout) + ":" + targetConverterClass.getName();
		Set<String> keywords = KEYWORD_CACHE.computeIfAbsent(cacheKey, (k) -> {
			Set<String> foundKeywords = new HashSet<>();
			// 此处存在由 Logback API 引起的、不可避免的 raw type 警告
			Map<String, Supplier<DynamicConverter>> supplierMap = layout.getDefaultConverterSupplierMap();
			for (Map.Entry<String, Supplier<DynamicConverter>> entry : supplierMap.entrySet()) {
				DynamicConverter instance = entry.getValue().get();
				if (instance != null && targetConverterClass.isAssignableFrom(instance.getClass())) {
					foundKeywords.add(entry.getKey());
				}
			}
			return foundKeywords;
		});
		return (node) -> node.getType() != 0 && node.getValue() != null
				&& keywords.contains(node.getValue().toString());
	}

	/**
	 * 将一个 Logback 节点树重新序列化为等效的日志格式字符串. 此方法能正确处理简单节点、复合节点以及带有格式化信息的节点.
	 * @param node 要重建的节点树的头节点.
	 * @return 重建后的日志格式字符串.
	 */
	public static String rebuildPattern(Node node) {
		StringBuilder sb = new StringBuilder();
		while (node != null) {
			if (node instanceof CompositeNode composite) {
				sb.append("%").append(composite.getValue());
				sb.append("(");
				if (composite.getChildNode() != null) {
					sb.append(rebuildPattern(composite.getChildNode()));
				}
				sb.append(")");
				List<String> options = composite.getOptions();
				if (options != null) {
					sb.append("{").append(String.join(",", options)).append("}");
				}
			}
			else if (node instanceof SimpleKeywordNode simple) {
				sb.append("%");
				FormatInfo format = simple.getFormatInfo();
				if (format != null && (format.getMin() != -1 || format.getMax() != Integer.MAX_VALUE)) {
					if (!format.isLeftPad()) {
						sb.append("-");
					}
					if (format.getMin() >= 0) {
						sb.append(format.getMin());
					}
					if (format.getMax() != Integer.MAX_VALUE) {
						sb.append(".").append(format.getMax());
					}
				}
				sb.append(simple.getValue());
				List<String> options = simple.getOptions();
				if (options != null) {
					sb.append("{").append(String.join(",", options)).append("}");
				}
			}
			else {
				sb.append(node.getValue());
			}
			node = node.getNext();
		}
		return sb.toString();
	}

	/**
	 * 定位注入操作的目标节点，这是一个带有特殊逻辑的内部查找方法. 如果目标在复合节点内部，它会返回该复合节点本身作为注入点.
	 * @param currentNode 搜索的起始节点.
	 * @param targetNodePredicate 用于识别注入目标的断言.
	 * @return 应该在其后进行注入的节点.
	 */
	static Node findInjectionTargetNode(Node currentNode, Predicate<Node> targetNodePredicate) {
		while (currentNode != null) {
			if (currentNode instanceof CompositeNode) {
				if (findNode(((CompositeNode) currentNode).getChildNode(), targetNodePredicate) != null) {
					return currentNode;
				}
			}
			if (targetNodePredicate.test(currentNode)) {
				return currentNode;
			}
			currentNode = currentNode.getNext();
		}
		return null;
	}

	/**
	 * 查找一个节点链表中的最后一个节点（尾节点）.
	 * @param head 链表的头节点.
	 * @return 链表的尾节点.
	 */
	private static Node findTail(Node head) {
		if (head == null) {
			return null;
		}
		Node current = head;
		while (current.getNext() != null) {
			current = current.getNext();
		}
		return current;
	}

}
