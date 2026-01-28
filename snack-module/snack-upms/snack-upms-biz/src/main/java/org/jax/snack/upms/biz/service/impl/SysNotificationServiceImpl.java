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
import org.jax.snack.upms.api.service.SysNotificationService;
import org.jax.snack.upms.api.vo.SysNotificationVO;
import org.jax.snack.upms.biz.converter.SysNotificationConverter;
import org.jax.snack.upms.biz.repository.SysNotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

/**
 * 站内信 Service 实现.
 *
 * @author Jax Jiang
 */
@Service
@RequiredArgsConstructor
public class SysNotificationServiceImpl implements SysNotificationService {

	private final SysNotificationRepository repository;

	private final SysNotificationConverter converter;

	@Override
	public PageResult<SysNotificationVO> queryByDsl(QueryCondition condition) {
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
