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
import org.jax.snack.lowcode.biz.excel.DropdownHandler;
import org.jax.snack.lowcode.biz.model.FieldDefinition;
import org.jax.snack.lowcode.biz.model.SchemaMetadata;
import org.jax.snack.lowcode.biz.options.OptionItem;
import org.jax.snack.lowcode.biz.options.OptionsResolver;
import org.jax.snack.lowcode.biz.service.crud.DynamicCrudService;
import org.jax.snack.lowcode.biz.service.crud.DynamicValidator;
import org.jax.snack.lowcode.biz.service.schema.SchemaService;
import tools.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
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

		// 构建表头 (支持多层)
		Map<String, List<String>> headers = buildHeadersMap(fields, schema);
		int headerRowCount = getHeaderRowCount(schema);

		// 构建下拉框配置
		List<DropdownHandler.DropdownConfig> dropdownConfigs = buildDropdownConfigs(fields);

		// 使用 ExcelBuilderFactory 写入
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		this.excelFactory.write(buffer,
				(ctx) -> ctx.sheet("数据",
						(sheetCtx) -> sheetCtx.headers(headers)
							.data(Collections.emptyList())
							.customize((ExcelWriterSheetBuilder builder) -> {
								if (!dropdownConfigs.isEmpty()) {
									builder.registerWriteHandler(new DropdownHandler(dropdownConfigs, headerRowCount));
								}
							})));

		// 输出文件
		ResponseHelper.downloadExcel(response, buffer, metadata.getLabel() + "_导入模板");

		log.info("导出模板: {}", schemaName);
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

		// 查询全部数据 (size 为 null 时返回全部)
		QueryCondition condition = new QueryCondition();
		PageResult<Map<String, Object>> pageResult = this.dynamicCrudService.queryPage(schemaName, condition);
		List<Map<String, Object>> dataList = pageResult.getRecords();

		// 构建表头 (支持多层)
		Map<String, List<String>> headers = buildHeadersMap(fields, schema);

		// 转换数据为列表格式
		List<List<Object>> rows = convertToRows(dataList, fields);

		// 使用 ExcelBuilderFactory 写入
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		this.excelFactory.write(buffer,
				(ctx) -> ctx.sheet("数据", (sheetCtx) -> sheetCtx.headers(headers).data(new ArrayList<>(rows))));

		// 输出文件
		ResponseHelper.downloadExcel(response, buffer, metadata.getLabel() + "_导出数据");

		log.info("导出数据: {}, 共 {} 条", schemaName, dataList.size());
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

		// 构建表头映射
		Map<String, FieldDefinition> headerMap = new HashMap<>();
		for (FieldDefinition field : fields) {
			headerMap.put(field.getFieldName(), field);
			headerMap.put(field.getLabel(), field);
		}

		// 成功计数
		AtomicInteger successCount = new AtomicInteger(0);

		// 创建 ExcelDataListener
		ExcelDataListener<Map<String, Object>, Map<Integer, String>> listener = new ExcelDataListener<>(
				// processor: 数据转换
				(rowData, colToHeader) -> {
					Map<String, Object> entityMap = new LinkedHashMap<>();

					rowData.forEach((index, val) -> {
						String headerName = colToHeader.get(index);
						if (headerName == null) {
							return;
						}

						FieldDefinition field = headerMap.get(headerName);
						if (field != null) {
							Object value;
							if (field.getXOptions() != null) {
								value = this.optionsResolver.getValue(field.getXOptions(), val);
							}
							else {
								value = convertValue(val, field.getType());
							}
							entityMap.put(field.getFieldName(), value);
						}
					});

					return entityMap.isEmpty() ? null : entityMap;
				}, null, // validator: 不用 Jakarta Validator
				// saveFunction: 批量保存
				(batch) -> {
					for (Map<String, Object> data : batch) {
						this.dynamicCrudService.create(schemaName, data);
						successCount.incrementAndGet();
					}
				},
				// businessValidator: JSON Schema 校验
				(data) -> this.dynamicValidator.validate(schemaName, data), 100, // batchSize
				false // failFast: 收集所有错误
		);

		// 执行读取
		FastExcel.read(file.getInputStream())
			.headRowNumber(headerRowCount)
			.registerReadListener(listener)
			.sheet()
			.doRead();

		Map<String, Object> result = new HashMap<>();
		result.put("successCount", successCount.get());

		log.info("导入数据: {}, 成功 {} 条", schemaName, successCount.get());
		return result;
	}

	private Map<String, List<String>> buildHeadersMap(List<FieldDefinition> fields, JsonNode schema) {
		JsonNode xExcel = schema.path("x-excel");
		JsonNode headersNode = xExcel.path("headers");

		Map<String, List<String>> headers = new LinkedHashMap<>();

		if (!headersNode.isMissingNode() && headersNode.isArray() && !headersNode.isEmpty()) {
			// 多层表头模式
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
			// 默认单层表头
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
				// 如果字段有 x-options 配置，翻译为标签
				if (value != null && field.getXOptions() != null) {
					value = this.optionsResolver.getLabel(field.getXOptions(), value);
				}
				row.add((value != null) ? value : "");
			}
			rows.add(row);
		}
		return rows;
	}

	private List<DropdownHandler.DropdownConfig> buildDropdownConfigs(List<FieldDefinition> fields) {
		List<DropdownHandler.DropdownConfig> configs = new ArrayList<>();

		// 计算公式列起始位置 (在所有字段列之后)
		int formulaColumnOffset = fields.size();
		int formulaColumnCounter = 0;

		for (int i = 0; i < fields.size(); i++) {
			FieldDefinition field = fields.get(i);
			if (field.getXOptions() != null) {
				List<OptionItem> options = this.optionsResolver.resolveOptions(field.getXOptions());

				if (!options.isEmpty()) {
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
		if (value == null || value.isEmpty()) {
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
