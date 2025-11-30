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

package org.jax.snack.framework.mybatisplus.query;

import lombok.Getter;
import lombok.Setter;

/**
 * 排序条件.
 *
 * @author Jax Jiang
 */
@Getter
@Setter
public class OrderByCondition {

	/**
	 * 排序字段名.
	 */
	private String field;

	/**
	 * 排序方向.
	 * <p>
	 * 枚举值: "asc" (升序) 或 "desc" (降序).
	 */
	private String direction;

}
