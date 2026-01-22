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

import org.hamcrest.Matchers;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.core.exception.constants.ErrorCode;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.framework.webtest.matcher.ExceptionMatchers;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.dto.SysIdRuleSegmentDTO;
import org.jax.snack.upms.api.dto.SysOrgDTO;
import org.jax.snack.upms.api.enums.ResetCycle;
import org.jax.snack.upms.api.enums.SegmentType;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.api.service.SysOrgService;
import org.jax.snack.upms.api.vo.SysOrgVO;
import org.jax.snack.upms.biz.entity.SysIdRule;
import org.jax.snack.upms.biz.entity.SysOrg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 组织架构 Controller 集成测试.
 *
 * @author Jax Jiang
 */
class SysOrgControllerTests extends UpmsIntegrationTests {

	private static final String API_ORGS = "/api/upms/orgs";

	private static final String API_ORGS_ID = "/api/upms/orgs/{id}";

	private static final String API_ORGS_IDS = "/api/upms/orgs/{ids}";

	private static final String API_ORGS_QUERY = "/api/upms/orgs/query";

	private static final String API_ORGS_TREE = "/api/upms/orgs/tree";

	private static final String API_ORGS_DESCENDANTS = "/api/upms/orgs/descendants";

	@Autowired
	private SysOrgService sysOrgService;

	@Autowired
	private SysIdRuleService sysIdRuleService;

	@BeforeEach
	void setupOrgCodeRule() {
		QueryCondition condition = QueryCondition.builder().eq(SysIdRule.Fields.ruleCode, "org_code").build();
		if (this.sysIdRuleService.queryByDsl(condition).getRecords().isEmpty()) {
			SysIdRuleDTO dto = new SysIdRuleDTO();
			dto.setRuleCode("org_code");
			dto.setRuleName("组织机构编码规则");
			dto.setResetCycle(ResetCycle.NEVER.name());

			SysIdRuleSegmentDTO seg1 = createFixedSegment();
			SysIdRuleSegmentDTO seg2 = createSequenceSegment();

			dto.setSegments(List.of(seg1, seg2));
			this.sysIdRuleService.create(dto);
		}
	}

	private SysOrgDTO buildDto(String parentCode) {
		SysOrgDTO dto = new SysOrgDTO();
		dto.setOrgName("测试机构");
		dto.setParentCode(parentCode);
		dto.setStatus(1);
		return dto;
	}

	private SysOrgVO queryFirst() {
		PageResult<SysOrgVO> result = this.sysOrgService.queryByDsl(QueryCondition.builder().build());
		if (result.getRecords().isEmpty()) {
			throw new IllegalStateException("Expected at least one org, but found none");
		}
		return result.getRecords().get(0);
	}

	private List<SysOrgVO> queryAll() {
		return this.sysOrgService.queryByDsl(QueryCondition.builder().build()).getRecords();
	}

	private SysIdRuleSegmentDTO createFixedSegment() {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.FIXED.name());
		seg.setConfig(Map.of("value", "ORG"));
		return seg;
	}

	private SysIdRuleSegmentDTO createSequenceSegment() {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.SEQUENCE.name());
		seg.setConfig(Map.of("length", 5));
		return seg;
	}

	@Nested
	class CreateOrg {

		@Test
		void shouldCreateRootOrg() throws Exception {
			postJson(API_ORGS, buildDto(null)).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysOrgVO vo = queryFirst();
			assertThat(vo).isNotNull();
			assertThat(vo.getOrgCode()).startsWith("ORG");
			assertThat(vo.getLevel()).isEqualTo(0);
			assertThat(vo.getAncestors()).isEmpty();
		}

		@Test
		void shouldCreateChildOrg() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String parentCode = queryFirst().getOrgCode();

			postJson(API_ORGS, buildDto(parentCode)).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			List<SysOrgVO> all = queryAll();
			SysOrgVO childVo = all.stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow();
			assertThat(childVo).isNotNull();
			assertThat(childVo.getParentCode()).isEqualTo(parentCode);
			assertThat(childVo.getAncestors()).isEqualTo(parentCode);
		}

		@Test
		void shouldFailWhenOrgNameBlank() throws Exception {
			SysOrgDTO dto = new SysOrgDTO();

			postJson(API_ORGS, dto).andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
				.andExpect(ExceptionMatchers.fieldHasError("orgName"));
		}

		@Test
		void shouldFailWhenParentNotFound() throws Exception {
			SysOrgDTO dto = buildDto("NON_EXISTENT_CODE");

			postJson(API_ORGS, dto).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class QueryOrgs {

		@Test
		void shouldReturnPaginatedResults() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));

			QueryCondition condition = QueryCondition.builder().current(1).size(10).build();

			postJson(API_ORGS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(2));
		}

		@Test
		void shouldFilterByOrgName() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));

			QueryCondition condition = QueryCondition.builder().eq(SysOrg.Fields.orgName, "测试机构").build();

			postJson(API_ORGS_QUERY, condition).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1));
		}

	}

	@Nested
	class GetById {

		@Test
		void shouldReturnOrgById() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			SysOrgVO created = queryFirst();

			getJson(API_ORGS_ID, created.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(PageResultMatchers.totalIs(1))
				.andExpect(PageResultMatchers.record(0, ".id", created.getId().intValue()))
				.andExpect(PageResultMatchers.record(0, ".orgCode", created.getOrgCode()))
				.andExpect(PageResultMatchers.record(0, ".orgName", "测试机构"));
		}

	}

	@Nested
	class TreeAndDescendants {

		@Test
		void shouldReturnMultiRootMultiLevelTree() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String root1Code = queryFirst().getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(root1Code));
			String child1Code = queryAll().stream()
				.filter((v) -> v.getLevel() == 1)
				.findFirst()
				.orElseThrow()
				.getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(child1Code));

			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));

			getJson(API_ORGS_TREE).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray())
				.andExpect(ApiResponseMatchers.data(".length()", 2));

			assertThat(queryAll()).hasSize(4);
		}

		@Test
		void shouldReturnSubtreeByOrgCode() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String root1Code = queryFirst().getOrgCode();
			SysOrgControllerTests.this.sysOrgService.create(buildDto(root1Code));

			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));

			getJson(API_ORGS_TREE + "?orgCodes=" + root1Code).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray())
				.andExpect(ApiResponseMatchers.data(".length()", 1))
				.andExpect(ApiResponseMatchers.data("[0].data.orgCode", root1Code))
				.andExpect(ApiResponseMatchers.data("[0].children.length()", 1));
		}

		@Test
		void shouldReturnDescendantsOfMultipleLevels() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String rootCode = queryFirst().getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(rootCode));
			String childCode = queryAll().stream()
				.filter((v) -> v.getLevel() == 1)
				.findFirst()
				.orElseThrow()
				.getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(childCode));
			String grandChildCode = queryAll().stream()
				.filter((v) -> v.getLevel() == 2)
				.findFirst()
				.orElseThrow()
				.getOrgCode();

			getJson(API_ORGS_DESCENDANTS + "?orgCodes=" + rootCode).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(ApiResponseMatchers.dataIsArray())
				.andExpect(ApiResponseMatchers.data(".length()", 3))
				.andExpect(jsonPath("$.data", Matchers.containsInAnyOrder(rootCode, childCode, grandChildCode)));
		}

	}

	@Nested
	class UpdateOrg {

		@Test
		void shouldUpdateOrgName() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			Long id = queryFirst().getId();

			SysOrgDTO updateDto = new SysOrgDTO();
			updateDto.setOrgName("新名称");

			putJson(API_ORGS_ID, updateDto, id).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(queryFirst().getOrgName()).isEqualTo("新名称");
		}

		@Test
		void shouldKeepParentCodeWhenNotProvided() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String parentCode = queryFirst().getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(parentCode));
			SysOrgVO childBefore = queryAll().stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow();

			SysOrgDTO updateDto = new SysOrgDTO();
			updateDto.setOrgName("更新后的子机构");

			putJson(API_ORGS_ID, updateDto, childBefore.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysOrgVO childAfter = queryAll().stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow();
			assertThat(childAfter.getOrgName()).isEqualTo("更新后的子机构");
			assertThat(childAfter.getParentCode()).isEqualTo(parentCode);
		}

		@Test
		void shouldCascadeDisableDescendants() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			SysOrgVO parentVo = queryFirst();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(parentVo.getOrgCode()));
			SysOrgVO childBefore = queryAll().stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow();
			assertThat(childBefore.getStatus()).isEqualTo(1);

			SysOrgDTO updateDto = new SysOrgDTO();
			updateDto.setStatus(0);

			putJson(API_ORGS_ID, updateDto, parentVo.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			SysOrgVO childAfter = queryAll().stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow();
			assertThat(childAfter.getStatus()).isEqualTo(0);
		}

		@Test
		void shouldMoveOrgAndDescendants() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String root1Code = queryFirst().getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(root1Code));
			String childCode = queryAll().stream()
				.filter((v) -> v.getLevel() == 1)
				.findFirst()
				.orElseThrow()
				.getOrgCode();
			Long childId = queryAll().stream().filter((v) -> v.getLevel() == 1).findFirst().orElseThrow().getId();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(childCode));
			String grandChildCode = queryAll().stream()
				.filter((v) -> v.getLevel() == 2)
				.findFirst()
				.orElseThrow()
				.getOrgCode();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			String root2Code = queryAll().stream()
				.filter((v) -> v.getLevel() == 0 && !v.getOrgCode().equals(root1Code))
				.findFirst()
				.orElseThrow()
				.getOrgCode();

			SysOrgDTO updateDto = new SysOrgDTO();
			updateDto.setParentCode(root2Code);

			putJson(API_ORGS_ID, updateDto, childId).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			List<SysOrgVO> all = queryAll();

			SysOrgVO childMoved = all.stream()
				.filter((v) -> v.getOrgCode().equals(childCode))
				.findFirst()
				.orElseThrow();
			assertThat(childMoved.getParentCode()).isEqualTo(root2Code);
			assertThat(childMoved.getLevel()).isEqualTo(1);
			assertThat(childMoved.getAncestors()).isEqualTo(root2Code);

			SysOrgVO grandChildMoved = all.stream()
				.filter((v) -> v.getOrgCode().equals(grandChildCode))
				.findFirst()
				.orElseThrow();
			assertThat(grandChildMoved.getLevel()).isEqualTo(2);
			assertThat(grandChildMoved.getAncestors()).isEqualTo(root2Code + "," + childCode);
		}

		@Test
		void shouldFailWhenNotFound() throws Exception {
			SysOrgDTO dto = new SysOrgDTO();
			dto.setOrgName("更新后的名称");

			putJson(API_ORGS_ID, dto, 99999L).andDo(print())
				.andExpect(status().isInternalServerError())
				.andExpect(ExceptionMatchers.code(ErrorCode.DATA_NOT_FOUND));
		}

	}

	@Nested
	class DeleteOrg {

		@Test
		void shouldDeleteOrgAndDescendants() throws Exception {
			SysOrgControllerTests.this.sysOrgService.create(buildDto(null));
			SysOrgVO parentVo = queryFirst();

			SysOrgControllerTests.this.sysOrgService.create(buildDto(parentVo.getOrgCode()));
			assertThat(queryAll()).hasSize(2);

			deleteJson(API_ORGS_IDS, parentVo.getId()).andDo(print())
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess());

			assertThat(queryAll()).isEmpty();
		}

	}

}
