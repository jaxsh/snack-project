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

package org.jax.snack.lowcode.biz.service.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import cn.idev.excel.FastExcel;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.result.PageResult;
import org.jax.snack.framework.excel.ExcelBuilderFactory;
import org.jax.snack.framework.excel.listener.ExcelDataListener;
import org.jax.snack.framework.excel.web.ResponseHelper;
import org.jax.snack.lowcode.api.service.DynamicCrudService;
import org.jax.snack.lowcode.biz.excel.DropdownHandler;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.options.OptionItem;
import org.jax.snack.lowcode.biz.options.OptionsResolver;
import org.jax.snack.lowcode.biz.service.crud.DynamicValidator;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 动态 Excel 服务.
 * <p>
 * 提供基于 Schema 的 Excel 导入导出功能.
 * </p>
 *
 * @author Jax Jiang
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicExcelService {

	private final SchemaService schemaService;

	private final DynamicCrudService dynamicCrudService;

	private final OptionsResolver optionsResolver;

	private final ExcelBuilderFactory excelFactory;

	private final DynamicValidator dynamicValidator;

	/**
	 * 导出模板.
	 * @param schemaName Schema 名称
	 * @param response HTTP 响应
	 * @throws IOException IO 异常
	 */
	public void exportTemplate(String schemaName, HttpServletResponse response) throws IOException {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);

		Map<String, List<String>> headers = buildHeadersMap(fields, schema);
		int headerRowCount = getHeaderRowCount(schema);

		List<DropdownHandler.DropdownConfig> dropdownConfigs = buildDropdownConfigs(fields);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		this.excelFactory.write(buffer,
				(ctx) -> ctx.sheet("数据",
						(sheetCtx) -> sheetCtx.headers(headers)
							.data(Collections.emptyList())
							.customize((ExcelWriterSheetBuilder builder) -> {
								if (!CollectionUtils.isEmpty(dropdownConfigs)) {
									builder.registerWriteHandler(new DropdownHandler(dropdownConfigs, headerRowCount));
								}
							})));

		ResponseHelper.downloadExcel(response, buffer, metadata.getLabel() + "_导入模板");

		log.info("Exporting template for: {}", schemaName);
	}

	/**
	 * 导出数据.
	 * @param schemaName Schema 名称
	 * @param response HTTP 响应
	 * @throws IOException IO 异常
	 */
	public void exportData(String schemaName, HttpServletResponse response) throws IOException {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		SchemaMetadata metadata = this.schemaService.extractMetadata(schema);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);

		QueryCondition condition = QueryCondition.builder().build();
		PageResult<Map<String, Object>> pageResult = this.dynamicCrudService.queryPage(schemaName, condition);
		List<Map<String, Object>> dataList = pageResult.getRecords();

		Map<String, List<String>> headers = buildHeadersMap(fields, schema);

		List<List<Object>> rows = convertToRows(dataList, fields);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		this.excelFactory.write(buffer,
				(ctx) -> ctx.sheet("数据", (sheetCtx) -> sheetCtx.headers(headers).data(new ArrayList<>(rows))));

		ResponseHelper.downloadExcel(response, buffer, metadata.getLabel() + "_导出数据");

		log.info("Exporting data for: {}, Total count: {}", schemaName, dataList.size());
	}

	/**
	 * 导入数据.
	 * @param schemaName Schema 名称
	 * @param file Excel 文件
	 * @return 导入结果
	 * @throws IOException IO 异常
	 */
	public Map<String, Object> importData(String schemaName, MultipartFile file) throws IOException {
		JsonNode schema = this.schemaService.getSchema(schemaName);
		List<FieldDefinition> fields = this.schemaService.extractVisibleFields(schema);
		int headerRowCount = getHeaderRowCount(schema);

		Map<String, FieldDefinition> headerMap = new HashMap<>();
		for (FieldDefinition field : fields) {
			headerMap.put(field.getFieldName(), field);
			headerMap.put(field.getLabel(), field);
		}

		AtomicInteger successCount = new AtomicInteger(0);

		ExcelDataListener<Map<String, Object>, Map<Integer, String>> listener = new ExcelDataListener<>(
				(rowData, colToHeader) -> {
					Map<String, Object> entityMap = new LinkedHashMap<>();

					rowData.forEach((index, val) -> {
						String headerName = colToHeader.get(index);
						if (headerName == null) {
							return;
						}

						FieldDefinition field = headerMap.get(headerName);
						if (!ObjectUtils.isEmpty(field)) {
							Object value;
							if (!ObjectUtils.isEmpty(field.getXOptions())) {
								value = this.optionsResolver.getValue(field.getXOptions(), val);
							}
							else {
								value = convertValue(val, field.getType());
							}
							entityMap.put(field.getFieldName(), value);
						}
					});

					return entityMap.isEmpty() ? null : entityMap;
				}, null, (batch) -> {
					for (Map<String, Object> data : batch) {
						this.dynamicCrudService.create(schemaName, data);
						successCount.incrementAndGet();
					}
				}, (data) -> this.dynamicValidator.validate(schemaName, data), 100, false);

		FastExcel.read(file.getInputStream())
			.headRowNumber(headerRowCount)
			.registerReadListener(listener)
			.sheet()
			.doRead();

		Map<String, Object> result = new HashMap<>();
		result.put("successCount", successCount.get());

		log.info("Importing data for: {}, Success count: {}", schemaName, successCount.get());
		return result;
	}

	private Map<String, List<String>> buildHeadersMap(List<FieldDefinition> fields, JsonNode schema) {
		JsonNode xExcel = schema.path("x-excel");
		JsonNode headersNode = xExcel.path("headers");

		Map<String, List<String>> headers = new LinkedHashMap<>();

		if (!headersNode.isMissingNode() && headersNode.isArray() && !headersNode.isEmpty()) {
			for (int colIdx = 0; colIdx < fields.size(); colIdx++) {
				FieldDefinition field = fields.get(colIdx);
				List<String> colHeaders = new ArrayList<>();
				for (JsonNode rowNode : headersNode) {
					if (rowNode.isArray() && colIdx < rowNode.size()) {
						colHeaders.add(rowNode.get(colIdx).asString(""));
					}
					else {
						colHeaders.add("");
					}
				}
				headers.put(field.getFieldName(), colHeaders);
			}
		}
		else {
			for (FieldDefinition field : fields) {
				headers.put(field.getFieldName(), List.of(field.getLabel()));
			}
		}

		return headers;
	}

	private int getHeaderRowCount(JsonNode schema) {
		JsonNode xExcel = schema.path("x-excel");
		JsonNode headersNode = xExcel.path("headers");

		if (!headersNode.isMissingNode() && headersNode.isArray()) {
			return headersNode.size();
		}
		return 1;
	}

	private List<List<Object>> convertToRows(List<Map<String, Object>> dataList, List<FieldDefinition> fields) {
		List<List<Object>> rows = new ArrayList<>();
		for (Map<String, Object> data : dataList) {
			List<Object> row = new ArrayList<>();
			for (FieldDefinition field : fields) {
				Object value = data.get(field.getFieldName());
				if (!ObjectUtils.isEmpty(value) && !ObjectUtils.isEmpty(field.getXOptions())) {
					value = this.optionsResolver.getLabel(field.getXOptions(), value);
				}
				row.add(!ObjectUtils.isEmpty(value) ? value : "");
			}
			rows.add(row);
		}
		return rows;
	}

	private List<DropdownHandler.DropdownConfig> buildDropdownConfigs(List<FieldDefinition> fields) {
		List<DropdownHandler.DropdownConfig> configs = new ArrayList<>();

		int formulaColumnOffset = fields.size();
		int formulaColumnCounter = 0;

		for (int i = 0; i < fields.size(); i++) {
			FieldDefinition field = fields.get(i);
			if (!ObjectUtils.isEmpty(field.getXOptions())) {
				List<OptionItem> options = this.optionsResolver.resolveOptions(field.getXOptions());

				if (!CollectionUtils.isEmpty(options)) {
					List<String[]> items = options.stream()
						.map((opt) -> new String[] { opt.label(), opt.valueAsString() })
						.toList();

					int formulaColIndex = formulaColumnOffset + formulaColumnCounter;
					formulaColumnCounter++;

					configs.add(new DropdownHandler.DropdownConfig(items, i, formulaColIndex));
				}
			}
		}

		return configs;
	}

	private Object convertValue(String value, String type) {
		if (ObjectUtils.isEmpty(value)) {
			return null;
		}
		return switch (type) {
			case "integer" -> Integer.parseInt(value);
			case "number" -> Double.parseDouble(value);
			case "boolean" -> Boolean.parseBoolean(value);
			default -> value;
		};
	}

}
