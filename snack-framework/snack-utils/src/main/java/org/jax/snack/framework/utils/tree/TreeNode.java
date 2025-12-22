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
import java.util.List;

import lombok.Getter;

/**
 * 通用树节点, 用于表示树形结构中的单个节点.
 *
 * @param <T> 节点数据类型
 * @author Jax Jiang
 */
@Getter
public class TreeNode<T> {

	private final T data;

	private final List<TreeNode<T>> children;

	public TreeNode(T data) {
		this.data = data;
		this.children = new ArrayList<>();
	}

	public void addChild(TreeNode<T> child) {
		this.children.add(child);
	}

	public boolean hasChildren() {
		return !this.children.isEmpty();
	}

}
