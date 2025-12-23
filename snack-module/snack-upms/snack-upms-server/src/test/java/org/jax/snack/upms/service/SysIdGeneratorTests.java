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
		// 1. 创建规则
		String code = "FULL_ID_TEST";
		SysIdRuleDTO dto = new SysIdRuleDTO();
		dto.setRuleCode(code);
		dto.setRuleName("完整测试");
		dto.setResetCycle(ResetCycle.DAILY.name());

		// Seg 1: CONST "SNACK-"
		SysIdRuleSegmentDTO seg1 = new SysIdRuleSegmentDTO();
		seg1.setSegmentType(SegmentType.FIXED.name());
		seg1.setConfig(Map.of("value", "SNACK-"));

		// Seg 2: DATE "yyyyMMdd"
		SysIdRuleSegmentDTO seg2 = new SysIdRuleSegmentDTO();
		seg2.setSegmentType(SegmentType.DATE.name());
		seg2.setConfig(Map.of("pattern", "yyyyMMdd"));

		// Seg 3: CONST "-"
		SysIdRuleSegmentDTO seg3 = new SysIdRuleSegmentDTO();
		seg3.setSegmentType(SegmentType.FIXED.name());
		seg3.setConfig(Map.of("value", "-"));

		// Seg 4: SEQ length=4
		SysIdRuleSegmentDTO seg4 = new SysIdRuleSegmentDTO();
		seg4.setSegmentType(SegmentType.SEQUENCE.name());
		seg4.setConfig(Map.of("length", 4));

		dto.setSegments(List.of(seg1, seg2, seg3, seg4));
		this.sysIdRuleService.create(dto);

		// 2. 生成并验证
		String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// 第一次生成
		String id1 = this.sysIdRuleService.generate(code);
		assertThat(id1).isEqualTo("SNACK-" + dateStr + "-0001");

		// 第二次生成
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

		// Seg 1: CONST "ORD-"
		SysIdRuleSegmentDTO seg1 = new SysIdRuleSegmentDTO();
		seg1.setSegmentType(SegmentType.FIXED.name());
		seg1.setConfig(Map.of("value", "ORD-"));

		// Seg 2: ARG "shopId"
		SysIdRuleSegmentDTO seg2 = new SysIdRuleSegmentDTO();
		seg2.setSegmentType(SegmentType.ARG.name());
		seg2.setConfig(Map.of("key", "shopId"));

		// Seg 3: SEQ
		SysIdRuleSegmentDTO seg3 = new SysIdRuleSegmentDTO();
		seg3.setSegmentType(SegmentType.SEQUENCE.name());
		seg3.setConfig(Map.of("length", 3));

		dto.setSegments(List.of(seg1, seg2, seg3));
		this.sysIdRuleService.create(dto);

		// 调用 1: 传入 shopId
		String id1 = this.sysIdRuleService.generate(code, Map.of("shopId", "9527"));
		assertThat(id1).isEqualTo("ORD-9527001");

		// 调用 2: 传入另一个 shopId
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

		// Seg: RANDOM length=6
		SysIdRuleSegmentDTO seg1 = new SysIdRuleSegmentDTO();
		seg1.setSegmentType(SegmentType.RANDOM.name());
		seg1.setConfig(Map.of("length", 6, "type", "numeric")); // 假设支持 type 参数，或默认
																// alphanumeric

		dto.setSegments(List.of(seg1));
		this.sysIdRuleService.create(dto);

		String id = this.sysIdRuleService.generate(code);
		assertThat(id).hasSize(6);
	}

}
