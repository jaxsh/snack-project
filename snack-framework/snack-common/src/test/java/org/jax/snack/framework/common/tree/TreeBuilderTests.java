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

package org.jax.snack.framework.common.tree;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TreeBuilder core scenario tests.
 *
 * @author Jax Jiang
 */
class TreeBuilderTests {

	private static final String COMPANY = "Company";

	private static final String IT_DEPT = "IT";

	private static final String HR_DEPT = "HR";

	private static final String DEV_TEAM = "Dev";

	private static final String QA_TEAM = "QA";

	private List<Department> departments;

	@BeforeEach
	void setUp() {
		this.departments = List.of(new Department(1L, COMPANY, null), new Department(2L, IT_DEPT, 1L),
				new Department(3L, HR_DEPT, 1L), new Department(4L, DEV_TEAM, 2L), new Department(5L, QA_TEAM, 2L),
				new Department(6L, "Backend", 4L));
	}

	@Test
	void scenario1_buildFullTree() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId);

		assertThat(tree).hasSize(1);

		TreeNode<Department> root = tree.get(0);
		assertThat(root.getData().id()).isEqualTo(1L);

		assertThat(root.getChildren()).extracting((node) -> node.getData().id()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void scenario2_buildWithRootAndOtherNodes() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId, 1L, 2L, 4L);

		assertThat(tree).hasSize(1);
		assertThat(tree.get(0).getData().id()).isEqualTo(1L);

		assertThat(tree.get(0).getChildren()).extracting((node) -> node.getData().id())
			.containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void scenario3_buildWithAncestorChain() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId, 2L, 4L, 6L);

		assertThat(tree).hasSize(1);

		TreeNode<Department> itDept = tree.get(0);
		assertThat(itDept.getData().id()).isEqualTo(2L);

		assertThat(itDept.getChildren()).extracting((node) -> node.getData().id()).containsExactlyInAnyOrder(4L, 5L);
	}

	@Test
	void scenario4_buildWithDifferentBranches() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId, 2L, 3L);

		assertThat(tree).hasSize(2).extracting((node) -> node.getData().id()).containsExactlyInAnyOrder(2L, 3L);

		TreeNode<Department> itDept = tree.stream()
			.filter((node) -> node.getData().id().equals(2L))
			.findFirst()
			.orElseThrow();

		assertThat(itDept.getChildren()).extracting((node) -> node.getData().id()).containsExactlyInAnyOrder(4L, 5L);
	}

	@Test
	void scenario5_buildWithCustomRootValue() {
		List<Department> customDepts = List.of(new Department(1L, COMPANY, 0L), new Department(2L, IT_DEPT, 1L),
				new Department(3L, HR_DEPT, 1L));

		List<TreeNode<Department>> tree = TreeBuilder.build(customDepts, 0L, Department::id, Department::parentId);

		assertThat(tree).hasSize(1);

		TreeNode<Department> root = tree.get(0);
		assertThat(root.getData().id()).isEqualTo(1L);

		assertThat(root.getChildren()).extracting((node) -> node.getData().id()).containsExactlyInAnyOrder(2L, 3L);
	}

	@Test
	void scenario6_findDescendants() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId);

		Set<Long> descendants = TreeBuilder.findDescendants(tree, Department::id, 2L);

		assertThat(descendants).containsExactlyInAnyOrder(2L, 4L, 5L, 6L);
	}

	@Test
	void scenario7_findDescendantsMultipleNodes() {
		List<TreeNode<Department>> tree = TreeBuilder.build(this.departments, null, Department::id,
				Department::parentId);

		Set<Long> descendants = TreeBuilder.findDescendants(tree, Department::id, 2L, 3L);

		assertThat(descendants).containsExactlyInAnyOrder(2L, 3L, 4L, 5L, 6L);
	}

	@Test
	void shouldThrowExceptionWhenItemsIsNull() {
		assertThatThrownBy(() -> TreeBuilder.build(null, null, Department::id, Department::parentId))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Items must not be null");
	}

	@Test
	void shouldThrowExceptionWhenIdExtractorIsNull() {
		assertThatThrownBy(() -> TreeBuilder.build(this.departments, null, null, Department::parentId))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("ID extractor must not be null");
	}

	@Test
	void shouldThrowExceptionWhenParentIdExtractorIsNull() {
		assertThatThrownBy(() -> TreeBuilder.build(this.departments, null, Department::id, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessageContaining("Parent ID extractor must not be null");
	}

	record Department(Long id, String name, Long parentId) {

	}

}
