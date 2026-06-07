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

import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.api.service.SysSessionService;
import org.jax.snack.upms.api.vo.SysSessionVO;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活跃 Session Controller.
 *
 * @author Jax Jiang
 */
@RestController
@RequestMapping("/api/upms/sessions")
@RequiredArgsConstructor
public class SysSessionController {

	private final SysSessionService service;

	/**
	 * 查询活跃 Session 列表.
	 * @param username 用户名，不传则返回所有用户
	 * @return Session 列表
	 */
	@GetMapping
	public List<SysSessionVO> list(@RequestParam(required = false) String username) {
		return this.service.getSessions(username);
	}

	/**
	 * 踢出指定 Session.
	 * @param sessionId Session ID
	 */
	@DeleteMapping("/{sessionId}")
	public void revoke(@PathVariable String sessionId) {
		this.service.revokeSession(sessionId);
	}

}
