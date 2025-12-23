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

package org.jax.snack.upms.controller;

import java.util.List;
import java.util.Map;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.QueryOperator;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.webtest.MockMvcTestSupport;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.dto.SysIdRuleSegmentDTO;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.api.vo.SysIdRuleVO;
import org.jax.snack.upms.biz.enums.ResetCycle;
import org.jax.snack.upms.biz.enums.SegmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ID 规则 Controller 集成测试.
 *
 * @author Jax Jiang
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class SysIdRuleControllerTests extends MockMvcTestSupport {

	private static final String BASE_URL = "/api/upms/id-rules";

	@Autowired
	private SysIdRuleService sysIdRuleService;

	@Nested
	@DisplayName("规则 CRUD 测试")
	class CrudTests {

		@Test
		@DisplayName("创建并查询规则")
		void shouldCreateAndQuery() throws Exception {
			// 1. 构造 DTO
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("TEST_ORDER_NO");
			dto.setRuleName("测试订单号");
			dto.setResetCycle(ResetCycle.DAILY.name());

			// 构造 segments
			SysIdRuleSegmentDTO seg1 = new SysIdRuleSegmentDTO();
			seg1.setSegmentType(SegmentType.FIXED.name());
			seg1.setConfig(Map.of("value", "ORD-"));

			SysIdRuleSegmentDTO seg2 = new SysIdRuleSegmentDTO();
			seg2.setSegmentType(SegmentType.SEQUENCE.name());
			seg2.setConfig(Map.of("length", 6));

			dto.setSegments(List.of(seg1, seg2));

			// 2. 创建
			postJson(BASE_URL, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			// 3. 查询验证
			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("ruleCode", Map.of(QueryOperator.EQ.getValue(), "TEST_ORDER_NO")));

			postJson(BASE_URL + "/query", condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".ruleName", "测试订单号"))
				// 验证 segments 数组长度为 2
				.andExpect(PageResultMatchers.record(0, ".segments.length()", 2));
		}

		@Test
		@DisplayName("更新规则")
		void shouldUpdateRule() throws Exception {
			// 1. 先创建一个
			SysIdRuleDTO createDto = new SysIdRuleDTO();
			createDto.setRuleCode("UPDATE_TEST");
			createDto.setRuleName("原名称");
			createDto.setResetCycle(ResetCycle.NEVER.name());
			// 必须提供有效的 segments，否则校验失败 400
			SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
			seg.setSegmentType(SegmentType.FIXED.name());
			seg.setConfig(Map.of("value", "TEMP"));
			createDto.setSegments(List.of(seg));

			postJson(BASE_URL, createDto).andExpect(status().isOk());

			// 2. 获取 ID
			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("ruleCode", Map.of(QueryOperator.EQ.getValue(), "UPDATE_TEST")));
			PageResult<SysIdRuleVO> result = SysIdRuleControllerTests.this.sysIdRuleService.queryByDsl(condition);
			Long id = result.getRecords().get(0).getId();

			// 3. 更新
			SysIdRuleDTO updateDto = new SysIdRuleDTO();
			updateDto.setRuleCode("UPDATE_TEST");
			updateDto.setRuleName("新名称");
			updateDto.setResetCycle(ResetCycle.NEVER.name());
			// 更新 segments
			updateDto.setSegments(List.of(seg));

			putJson(BASE_URL + "/{id}", updateDto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			// 4. 验证更新结果
			SysIdRuleVO vo = SysIdRuleControllerTests.this.sysIdRuleService.getById(id);
			if (!"新名称".equals(vo.getRuleName())) {
				throw new AssertionError("Update failed");
			}
		}

	}

}
