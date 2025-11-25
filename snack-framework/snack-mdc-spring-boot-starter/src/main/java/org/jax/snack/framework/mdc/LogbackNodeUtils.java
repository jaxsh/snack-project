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
 * Logback Pattern 节点操作工具类.
 * <p>
 * 提供对 Logback {@link ch.qos.logback.core.pattern.parser.Node} 链表的底层操作能力，
 * 用于解析、查找、注入和重建日志格式字符串.
 *
 * @author Jax Jiang
 */
public final class LogbackNodeUtils {

	/**
	 * Converter 关键字缓存.
	 * <p>
	 * 缓存 PatternLayout 实例 + Converter 类名对应的关键字集合（例如 {@code ThreadConverter} ->
	 * {@code {"t", "thread"}}）. 避免重复反射查找，提升性能.
	 */
	private static final ConcurrentMap<String, Set<String>> KEYWORD_CACHE = new ConcurrentHashMap<>();

	private LogbackNodeUtils() {
	}

	/**
	 * 递归查找节点.
	 * <p>
	 * 深度优先搜索，支持遍历复合节点（CompositeNode）.
	 * @param node 起始节点
	 * @param predicate 匹配条件
	 * @return 匹配的节点，未找到返回 null
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
	 * 递归注入节点.
	 * <p>
	 * 在目标节点之后插入新的节点链表. 如果目标节点位于复合节点内，也会正确处理.
	 * @param topNode 根节点
	 * @param injectHead 待注入节点链表的头节点
	 * @param targetNodePredicate 目标节点匹配条件
	 * @return 注入成功返回 true
	 */
	public static boolean recursiveInject(Node topNode, Node injectHead, Predicate<Node> targetNodePredicate) {
		Node injectionTarget = findInjectionTargetNode(topNode, targetNodePredicate);

		if (injectionTarget == null) {
			return false;
		}

		Node insertionPointPrev = injectionTarget;
		Node walker = injectionTarget.getNext();

		// 跳过目标后的字面量节点（如空格），寻找最佳插入点
		while (walker != null) {
			// 0 = LITERAL
			if (walker.getType() != 0) {
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
	 * 创建 Converter 匹配断言.
	 * <p>
	 * 生成一个 Predicate，用于判断某个 Node 是否对应指定的 Converter 类.
	 * @param targetConverterClass 目标 Converter 类
	 * @param layout 当前 PatternLayout
	 * @return 节点断言
	 */
	@SuppressWarnings("rawtypes")
	public static Predicate<Node> getNodePredicateForConverter(Class<?> targetConverterClass, PatternLayout layout) {
		String cacheKey = System.identityHashCode(layout) + ":" + targetConverterClass.getName();
		Set<String> keywords = KEYWORD_CACHE.computeIfAbsent(cacheKey, (k) -> {
			Set<String> foundKeywords = new HashSet<>();
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
	 * 重建日志格式字符串.
	 * <p>
	 * 将 Node 链表序列化回字符串格式，支持处理参数和格式修饰符.
	 * @param node 链表头节点
	 * @return 日志格式字符串
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
	 * 查找注入目标节点.
	 * @param currentNode 当前节点
	 * @param targetNodePredicate 目标匹配条件
	 * @return 目标节点，未找到返回 null
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
	 * 查找链表尾节点.
	 * @param head 头节点
	 * @return 尾节点
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
