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

package org.jax.snack.upms.biz.controller;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.upms.api.service.SysNotificationService;
import org.jax.snack.upms.api.vo.SysNotificationVO;
import org.jax.snack.upms.biz.entity.SysNotification;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内信控制器.
 *
 * @author Jax Jiang
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class SysNotificationController {

	private final SysNotificationService sysNotificationService;

	/**
	 * 分页查询站内信 (DSL).
	 * @param condition 查询条件
	 * @return 分页列表
	 */
	@PostMapping("/query")
	public PageResult<SysNotificationVO> query(@RequestBody QueryCondition condition) {
		return this.sysNotificationService.queryByDsl(condition);
	}

	/**
	 * 删除站内信.
	 * @param ids ID 列表
	 */
	@DeleteMapping("/{ids}")
	public void removeById(@PathVariable List<Long> ids) {
		WhereCondition condition = WhereCondition.builder().in(SysNotification.Fields.id, ids).build();
		this.sysNotificationService.deleteByDsl(condition);
	}

	/**
	 * 通用更新站内信 (DSL).
	 * @param data 更新数据 (Map)
	 * @param condition 更新条件
	 */
	@PutMapping("/update")
	public void putByDsl(@RequestBody Map<String, Object> data, @RequestBody WhereCondition condition) {
		this.sysNotificationService.updateByDsl(data, condition);
	}

}
