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

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.service.SysMessageLogService;
import org.jax.snack.upms.api.vo.SysMessageLogVO;
import org.jax.snack.upms.biz.converter.SysMessageLogConverter;
import org.jax.snack.upms.biz.repository.SysMessageLogRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 消息发送日志 Service 实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysMessageLogServiceImpl implements SysMessageLogService {

	private final SysMessageLogRepository repository;

	private final SysMessageLogConverter converter;

	@Override
	public PageResult<SysMessageLogVO> queryByDsl(QueryCondition condition) {
		if (!ObjectUtils.isEmpty(condition.getSize())) {
			return this.converter.toPageResult(this.repository.queryPageByDsl(condition));
		}
		else {
			return this.converter.toPageResult(this.repository.queryListByDsl(condition));
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteByDsl(WhereCondition condition) {
		this.repository.deleteByDsl(condition);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateByDsl(Map<String, Object> data, WhereCondition condition) {
		this.repository.updateByDsl(data, condition);
	}

}
