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

package org.jax.snack.lowcode.biz.liteflow.component;

import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.exception.BusinessException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 业务错误组件.
 * <p>
 * 主动抛出异常以中断流程并触发回滚. Tag 格式: "errorCode:errorMessage"
 *
 * @author Jax Jiang
 */
@Slf4j
@Component("errorCmp")
public class ErrorComponent extends NodeComponent {

	@Override
	public void process() throws Exception {
		String tag = getTag();
		if (!StringUtils.hasText(tag)) {
			throw new BusinessException("500", "Business execution error");
		}

		String[] parts = tag.split(":", 2);
		String code = parts[0];
		String message = (parts.length > 1) ? parts[1] : "Business error";

		log.warn("Flow interrupted by errorCmp: {} - {}", code, message);
		throw new BusinessException(code, message);
	}

}
