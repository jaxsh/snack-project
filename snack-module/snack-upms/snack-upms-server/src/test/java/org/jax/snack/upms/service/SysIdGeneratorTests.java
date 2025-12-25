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

package org.jax.snack.upms.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.jax.snack.upms.api.dto.SysIdRuleDTO;
import org.jax.snack.upms.api.dto.SysIdRuleSegmentDTO;
import org.jax.snack.upms.api.service.SysIdRuleService;
import org.jax.snack.upms.biz.enums.ResetCycle;
import org.jax.snack.upms.biz.enums.SegmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ID 生成核心逻辑集成测试.
 *
 * @author Jax Jiang
 */
@SpringBootTest
@Transactional
class SysIdGeneratorTests {

	@Autowired
	private SysIdRuleService sysIdRuleService;

	@Test
	@DisplayName("测试完整 ID 生成: 前缀 + 日期 + 序列号")
	void shouldGenerateFullId() {
		String code = "FULL_ID_TEST";
		SysIdRuleDTO dto = new SysIdRuleDTO();
		dto.setRuleCode(code);
		dto.setRuleName("完整测试");
		dto.setResetCycle(ResetCycle.DAILY.name());

		SysIdRuleSegmentDTO seg1 = createFixedSegment("SNACK-");
		SysIdRuleSegmentDTO seg2 = createDateSegment("yyyyMMdd");
		SysIdRuleSegmentDTO seg3 = createFixedSegment("-");
		SysIdRuleSegmentDTO seg4 = createSequenceSegment(4);

		dto.setSegments(List.of(seg1, seg2, seg3, seg4));
		this.sysIdRuleService.create(dto);

		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		String id1 = this.sysIdRuleService.generate(code);
		assertThat(id1).isEqualTo("SNACK-" + dateStr + "-0001");

		String id2 = this.sysIdRuleService.generate(code);
		assertThat(id2).isEqualTo("SNACK-" + dateStr + "-0002");
	}

	@Test
	@DisplayName("测试参数注入 (Arg)")
	void shouldInjectArgs() {
		String code = "ARG_TEST";
		SysIdRuleDTO dto = new SysIdRuleDTO();
		dto.setRuleCode(code);
		dto.setRuleName("参数测试");
		dto.setResetCycle(ResetCycle.NEVER.name());

		SysIdRuleSegmentDTO seg1 = createFixedSegment("ORD-");
		SysIdRuleSegmentDTO seg2 = createArgSegment("shopId");
		SysIdRuleSegmentDTO seg3 = createSequenceSegment(3);

		dto.setSegments(List.of(seg1, seg2, seg3));
		this.sysIdRuleService.create(dto);

		String id1 = this.sysIdRuleService.generate(code, Map.of("shopId", "9527"));
		assertThat(id1).isEqualTo("ORD-9527001");

		String id2 = this.sysIdRuleService.generate(code, Map.of("shopId", "8888"));
		assertThat(id2).isEqualTo("ORD-8888002");
	}

	@Test
	@DisplayName("测试随机串 (Random)")
	void shouldGenerateRandom() {
		String code = "RANDOM_TEST";
		SysIdRuleDTO dto = new SysIdRuleDTO();
		dto.setRuleCode(code);
		dto.setRuleName("随机测试");
		dto.setResetCycle(ResetCycle.NEVER.name());

		SysIdRuleSegmentDTO seg1 = createRandomSegment(6, "numeric");

		dto.setSegments(List.of(seg1));
		this.sysIdRuleService.create(dto);

		String id = this.sysIdRuleService.generate(code);
		assertThat(id).hasSize(6);
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

	private SysIdRuleSegmentDTO createDateSegment(String pattern) {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.DATE.name());
		seg.setConfig(Map.of("pattern", pattern));
		return seg;
	}

	private SysIdRuleSegmentDTO createRandomSegment(int length, String type) {
		SysIdRuleSegmentDTO seg = new SysIdRuleSegmentDTO();
		seg.setSegmentType(SegmentType.RANDOM.name());
		seg.setConfig(Map.of("length", length, "type", type));
		return seg;
	}

}
