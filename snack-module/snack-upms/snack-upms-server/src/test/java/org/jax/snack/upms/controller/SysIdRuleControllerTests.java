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

import static org.assertj.core.api.Assertions.assertThat;
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

	private static final String API_ID_RULES = "/api/upms/id-rules";

	private static final String API_ID_RULES_QUERY = "/api/upms/id-rules/query";

	private static final String API_ID_RULES_ID = "/api/upms/id-rules/{id}";

	@Autowired
	private SysIdRuleService sysIdRuleService;

	private SysIdRuleVO queryByRuleCode(String ruleCode) {
		QueryCondition condition = new QueryCondition();
		condition.setWhere(Map.of("ruleCode", Map.of(QueryOperator.EQ.getValue(), ruleCode)));
		PageResult<SysIdRuleVO> result = this.sysIdRuleService.queryByDsl(condition);
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("Rule not found: " + ruleCode);
		}
		return result.getRecords().get(0);
	}

	private SysIdRuleSegmentDTO createFixedSegment(String value) {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.FIXED.name());
		seg.setConfig(Map.of("value", value));
		return seg;
	}

	private SysIdRuleSegmentDTO createSequenceSegment(int length) {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.SEQUENCE.name());
		seg.setConfig(Map.of("length", length));
		return seg;
	}

	private SysIdRuleSegmentDTO createArgSegment(String key) {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.ARG.name());
		seg.setConfig(Map.of("key", key));
		return seg;
	}

	@Nested
	@DisplayName("规则 CRUD 测试")
	class CrudTests {

		@Test
		@DisplayName("创建并查询规则")
		void shouldCreateAndQuery() throws Exception {
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("TEST_ORDER_NO");
			dto.setRuleName("测试订单号");
			dto.setResetCycle(ResetCycle.DAILY.name());

			SysIdRuleSegmentDTO seg1 = createFixedSegment("ORD-");
			SysIdRuleSegmentDTO seg2 = createSequenceSegment(6);

			dto.setSegments(List.of(seg1, seg2));

			postJson(API_ID_RULES, dto).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("ruleCode", Map.of(QueryOperator.EQ.getValue(), "TEST_ORDER_NO")));

			postJson(API_ID_RULES_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".ruleName", "测试订单号"))
				.andExpect(PageResultMatchers.record(0, ".segments.length()", 2));
		}

		@Test
		@DisplayName("更新规则")
		void shouldUpdateRule() throws Exception {
			SysIdRuleDTO createDto = new SysIdRuleDTO();
			createDto.setRuleCode("UPDATE_TEST");
			createDto.setRuleName("原名称");
			createDto.setResetCycle(ResetCycle.NEVER.name());

			SysIdRuleSegmentDTO seg = createFixedSegment("TEMP");
			createDto.setSegments(List.of(seg));

			postJson(API_ID_RULES, createDto).andExpect(status().isOk());

			Long id = queryByRuleCode("UPDATE_TEST").getId();

			SysIdRuleDTO updateDto = new SysIdRuleDTO();
			updateDto.setRuleCode("UPDATE_TEST");
			updateDto.setRuleName("新名称");
			updateDto.setResetCycle(ResetCycle.NEVER.name());
			updateDto.setSegments(List.of(seg));

			putJson(API_ID_RULES_ID, updateDto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysIdRuleVO vo = SysIdRuleControllerTests.this.sysIdRuleService.getById(id);
			assertThat(vo.getRuleName()).isEqualTo("新名称");
		}

		@Test
		@DisplayName("按 ID 查询规则")
		void shouldReturnRuleById() throws Exception {
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("GET_BY_ID_TEST");
			dto.setRuleName("ID查询测试");
			dto.setResetCycle(ResetCycle.NEVER.name());

			SysIdRuleSegmentDTO seg = createFixedSegment("ID-");
			dto.setSegments(List.of(seg));

			postJson(API_ID_RULES, dto).andExpect(status().isOk());
			Long id = queryByRuleCode("GET_BY_ID_TEST").getId();

			getJson(API_ID_RULES_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.data(".ruleCode", "GET_BY_ID_TEST"))
				.andExpect(ApiResponseMatchers.data(".ruleName", "ID查询测试"));
		}

		@Test
		@DisplayName("删除规则")
		void shouldDeleteRule() throws Exception {
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("DELETE_TEST");
			dto.setRuleName("删除测试");
			dto.setResetCycle(ResetCycle.NEVER.name());

			SysIdRuleSegmentDTO seg = createFixedSegment("DEL-");
			dto.setSegments(List.of(seg));

			postJson(API_ID_RULES, dto).andExpect(status().isOk());
			Long id = queryByRuleCode("DELETE_TEST").getId();

			deleteJson(API_ID_RULES_ID, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			QueryCondition condition = new QueryCondition();
			condition.setWhere(Map.of("ruleCode", Map.of(QueryOperator.EQ.getValue(), "DELETE_TEST")));
			assertThat(SysIdRuleControllerTests.this.sysIdRuleService.queryByDsl(condition).getRecords()).isEmpty();
		}

	}

	@Nested
	@DisplayName("ID 生成测试")
	class GenerateTests {

		@Test
		@DisplayName("按 shortName 分组生成独立序号")
		void shouldGenerateSequenceGroupedByShortName() {
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("shortname_test");
			dto.setRuleName("按简称分组测试");
			dto.setResetCycle(ResetCycle.NEVER.name());

			SysIdRuleSegmentDTO seg1 = createFixedSegment("ORG-");
			SysIdRuleSegmentDTO seg2 = createArgSegment("shortName");
			SysIdRuleSegmentDTO seg3 = createFixedSegment("-");
			SysIdRuleSegmentDTO seg4 = createSequenceSegment(5);

			dto.setSegments(List.of(seg1, seg2, seg3, seg4));
			SysIdRuleControllerTests.this.sysIdRuleService.create(dto);

			String ruleCode = "shortname_test";
			String argName = "shortName";

			String hq1 = SysIdRuleControllerTests.this.sysIdRuleService.generate(ruleCode, Map.of(argName, "HQ"));
			String hq2 = SysIdRuleControllerTests.this.sysIdRuleService.generate(ruleCode, Map.of(argName, "HQ"));
			String sh1 = SysIdRuleControllerTests.this.sysIdRuleService.generate(ruleCode, Map.of(argName, "SH"));
			String hq3 = SysIdRuleControllerTests.this.sysIdRuleService.generate(ruleCode, Map.of(argName, "HQ"));

			assertThat(hq1).isEqualTo("ORG-HQ-00001");
			assertThat(hq2).isEqualTo("ORG-HQ-00002");
			assertThat(sh1).isEqualTo("ORG-SH-00001");
			assertThat(hq3).isEqualTo("ORG-HQ-00003");
		}

	}

}
