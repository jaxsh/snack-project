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

package org.jax.snack.lowcode.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.exception.BusinessException;
import org.jax.snack.framework.webtest.MockMvcTestSupport;
import org.jax.snack.framework.webtest.matcher.PageResultMatchers;
import org.jax.snack.lowcode.api.dto.LowcodeSchemaDTO;
import org.jax.snack.lowcode.api.service.DynamicCrudService;
import org.jax.snack.lowcode.api.service.LowcodeSchemaService;
import org.jax.snack.lowcode.api.vo.LowcodeSchemaVO;
import org.jax.snack.lowcode.biz.entity.LowcodeSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 动态 CRUD 端到端测试.
 *
 * @author Jax Jiang
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("lowcode")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DynamicCrudE2ETests extends MockMvcTestSupport {

	private static final String API_DYNAMIC = "/api/lowcode/{resourcePath}";

	private static final String API_DYNAMIC_BY_ID = "/api/lowcode/{resourcePath}/{id}";

	private static final String API_DYNAMIC_QUERY = "/api/lowcode/{resourcePath}/query";

	private static final String API_DYNAMIC_SCHEMA = "/api/lowcode/{resourcePath}/schema";

	@Autowired
	private LowcodeSchemaService schemaService;

	@Autowired
	private DynamicCrudService dynamicCrudService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void tearDown() {
		cleanup("e2e1", "lc_e2e1");
		cleanup("e2e2", "lc_e2e2");
		cleanup("e2e3", "lc_e2e3");
		cleanup("e2e4", "lc_e2e4");
		cleanup("e2e5", "lc_e2e5");
		cleanup("e2e6", "lc_e2e6");
		cleanup("e2e7", "lc_e2e7");
	}

	private void cleanup(String schema, String table) {
		try {
			this.jdbcTemplate.execute("DROP TABLE IF EXISTS " + table);
		}
		catch (DataAccessException ignored) {
		}

		try {
			QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.schemaName, schema).build();
			this.schemaService.deleteByDsl(condition);
		}
		catch (DataAccessException | BusinessException ignored) {
		}
	}

	private void setupSchema(String name, String path, String table, String field) {
		LowcodeSchemaDTO dto = new LowcodeSchemaDTO();
		dto.setSchemaName(name);
		dto.setResourcePath(path);
		dto.setTableName(table);
		dto.setLabel("E2E");

		LowcodeSchemaDTO.FieldDTO f = new LowcodeSchemaDTO.FieldDTO();
		f.setFieldName(field);
		f.setDbColumn(field);
		f.setTitle("T");
		f.setLogicType("string");
		f.setLength(100);
		dto.setFields(List.of(f));

		this.schemaService.create(dto);

		QueryCondition condition = QueryCondition.builder().eq(LowcodeSchema.Fields.schemaName, name).build();
		LowcodeSchemaVO vo = this.schemaService.queryByDsl(condition).getRecords().get(0);

		this.schemaService.publishSchema(vo.getId());
	}

	@Test
	@Order(1)
	void shouldGetSchemaDefinition() throws Exception {
		String n = "e2e1";
		String p = "e2ep1";
		String f = "fget";
		setupSchema(n, p, "lc_e2e1", f);
		getJson(API_DYNAMIC_SCHEMA, p).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.properties." + f).exists());
	}

	@Test
	@Order(2)
	void shouldCreateRecord() throws Exception {
		String n = "e2e2";
		String p = "e2ep2";
		String f = "fcreate";
		setupSchema(n, p, "lc_e2e2", f);
		Map<String, Object> data = Map.of(f, "v1");
		postJson(API_DYNAMIC, data, p).andExpect(status().isOk());

		QueryCondition cond = QueryCondition.builder().size(1).build();
		assertThat(this.dynamicCrudService.queryPage(n, cond).getRecords()).isNotEmpty();
	}

	@Test
	@Order(3)
	void shouldQueryRecords() throws Exception {
		String n = "e2e3";
		String p = "e2ep3";
		String f = "fquery";
		setupSchema(n, p, "lc_e2e3", f);
		this.dynamicCrudService.create(n, new HashMap<>(Map.of(f, "v2")));

		QueryCondition cond = QueryCondition.builder().size(10).build();
		postJson(API_DYNAMIC_QUERY, cond, p).andExpect(status().isOk()).andExpect(PageResultMatchers.isNotEmpty());
	}

	@Test
	@Order(4)
	void shouldGetRecordById() throws Exception {
		String n = "e2e4";
		String p = "e2ep4";
		String f = "fbyid";
		setupSchema(n, p, "lc_e2e4", f);
		HashMap<String, Object> data = new HashMap<>(Map.of(f, "v3"));
		this.dynamicCrudService.create(n, data);
		Long id = ((Number) data.get("id")).longValue();

		getJson(API_DYNAMIC_BY_ID, p, id).andExpect(status().isOk()).andExpect(jsonPath("$." + f).value("v3"));
	}

	@Test
	@Order(5)
	void shouldReturn404WhenNotFound() throws Exception {
		String n = "e2e5";
		String p = "e2ep5";
		String f = "f404";
		setupSchema(n, p, "lc_e2e5", f);
		getJson(API_DYNAMIC_BY_ID, p, 999L).andExpect(status().isNotFound());
	}

	@Test
	@Order(6)
	void shouldUpdateRecord() throws Exception {
		String n = "e2e6";
		String p = "e2ep6";
		String f = "fupdate";
		setupSchema(n, p, "lc_e2e6", f);
		HashMap<String, Object> data = new HashMap<>(Map.of(f, "v4"));
		this.dynamicCrudService.create(n, data);
		Long id = ((Number) data.get("id")).longValue();

		Map<String, Object> payload = Map.of(f, "v5");
		putJson(API_DYNAMIC_BY_ID, payload, p, id).andExpect(status().isOk());

		var row = this.dynamicCrudService.getById(n, id).orElseThrow();
		assertThat(row.get(f)).isEqualTo("v5");
	}

	@Test
	@Order(7)
	void shouldDeleteRecord() throws Exception {
		String n = "e2e7";
		String f = "fdelete";
		setupSchema(n, n, "lc_e2e7", f);
		HashMap<String, Object> data = new HashMap<>(Map.of(f, "v6"));
		this.dynamicCrudService.create(n, data);
		Long id = ((Number) data.get("id")).longValue();

		deleteJson(API_DYNAMIC_BY_ID, n, id).andExpect(status().isOk());
		assertThat(this.dynamicCrudService.getById(n, id)).isEmpty();
	}

}
