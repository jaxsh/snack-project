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

package org.jax.snack.lowcode.biz.options;

import java.util.List;

/**
 * 字典数据解析器接口.
 * <p>
 * 由 UPMS 模块提供实现.
 * </p>
 *
 * @author Jax Jiang
 */
@FunctionalInterface
public interface DictDataResolver {

	/**
	 * 根据字典类型获取选项列表.
	 * @param dictType 字典类型
	 * @return 选项列表 [label, value]
	 */
	List<OptionItem> resolve(String dictType);

}
