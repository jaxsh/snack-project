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

package org.jax.snack.upms.biz.service.impl;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.service.SysLoginLogService;
import org.jax.snack.upms.api.vo.SysLoginLogVO;
import org.jax.snack.upms.biz.converter.SysLoginLogConverter;
import org.jax.snack.upms.biz.repository.SysLoginLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

/**
 * 登录日志 Service 实现类.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysLoginLogServiceImpl implements SysLoginLogService {

	private final SysLoginLogRepository repository;

	private final SysLoginLogConverter converter;

	@Override
	public PageResult<SysLoginLogVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

}
