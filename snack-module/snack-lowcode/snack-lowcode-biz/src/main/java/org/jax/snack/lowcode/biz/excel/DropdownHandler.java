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

package org.jax.snack.lowcode.biz.excel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.handler.context.SheetWriteHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddressList;

/**
 * 字典下拉框处理器.
 * <p>
 * 在 Excel 模板中创建下拉框约束，并支持 VLOOKUP 公式列.
 * </p>
 *
 * @author Jax Jiang
 */
public class DropdownHandler implements SheetWriteHandler {

	private static final String DICT_SHEET_NAME = "DICT";

	private final List<DropdownConfig> configs;

	private final int headerRowCount;

	private final Map<DropdownConfig, String> nameRefs = new HashMap<>();

	private final Map<DropdownConfig, int[]> dictRanges = new HashMap<>();

	private int nameCounter = 0;

	/**
	 * 构造器.
	 * @param configs 下拉框配置列表
	 * @param headerRowCount 表头行数
	 */
	public DropdownHandler(List<DropdownConfig> configs, int headerRowCount) {
		this.configs = configs;
		this.headerRowCount = headerRowCount;
	}

	@Override
	public void afterSheetCreate(SheetWriteHandlerContext ctx) {
		Sheet mainSheet = ctx.getWriteSheetHolder().getSheet();

		writeDictDataAndCreateDropdowns(ctx.getWriteWorkbookHolder().getWorkbook(), mainSheet);
		presetFormulaColumns(mainSheet);
		hideSheet(ctx.getWriteWorkbookHolder().getWorkbook(), DICT_SHEET_NAME);
	}

	private void writeDictDataAndCreateDropdowns(Workbook workbook, Sheet mainSheet) {
		Sheet dictSheet = workbook.getSheet(DICT_SHEET_NAME);
		if (dictSheet == null) {
			dictSheet = workbook.createSheet(DICT_SHEET_NAME);
		}

		int currentRow = dictSheet.getLastRowNum() + 1;
		if (currentRow == 1 && dictSheet.getRow(0) == null) {
			currentRow = 0;
		}

		int maxRow = SpreadsheetVersion.EXCEL2007.getMaxRows() - 1;
		DataValidationHelper helper = mainSheet.getDataValidationHelper();

		for (DropdownConfig config : this.configs) {
			int startRow = currentRow;
			List<String[]> items = config.getItems();

			for (String[] item : items) {
				Row row = dictSheet.createRow(currentRow);
				row.createCell(0).setCellValue(item[0]); // label
				row.createCell(1).setCellValue(item[1]); // value
				currentRow++;
			}

			// 记录范围 (Excel 行号从 1 开始)
			this.dictRanges.put(config, new int[] { startRow + 1, currentRow });

			String nameRef = "N_" + this.nameCounter;
			this.nameCounter++;
			this.nameRefs.put(config, nameRef);

			Name name = workbook.createName();
			name.setNameName(nameRef);
			name.setRefersToFormula(DICT_SHEET_NAME + "!$A$" + (startRow + 1) + ":$A$" + currentRow);

			DataValidation validation = helper.createValidation(helper.createFormulaListConstraint(nameRef),
					new CellRangeAddressList(this.headerRowCount, maxRow, config.getDropdownColumnIndex(),
							config.getDropdownColumnIndex()));
			validation.setSuppressDropDownArrow(true);
			mainSheet.addValidationData(validation);
		}
	}

	private void presetFormulaColumns(Sheet mainSheet) {
		for (DropdownConfig config : this.configs) {
			Integer formulaCol = config.getFormulaColumnIndex();
			if (formulaCol == null) {
				continue;
			}

			int[] range = this.dictRanges.get(config);
			if (range == null) {
				continue;
			}

			String dropdownColLetter = getColumnLetter(config.getDropdownColumnIndex());

			// 在数据起始行设置公式
			int dataStartRowIndex = this.headerRowCount;
			Row row = mainSheet.getRow(dataStartRowIndex);
			if (row == null) {
				row = mainSheet.createRow(dataStartRowIndex);
			}
			Cell cell = row.getCell(formulaCol);
			if (cell == null) {
				cell = row.createCell(formulaCol);
			}
			int excelRowNumber = dataStartRowIndex + 1;
			String formula = String.format("IFERROR(VLOOKUP(%s%d,%s!$A$%d:$B$%d,2,0),\"\")", dropdownColLetter,
					excelRowNumber, DICT_SHEET_NAME, range[0], range[1]);
			cell.setCellFormula(formula);

			// 隐藏公式列
			mainSheet.setColumnHidden(formulaCol, true);
		}
	}

	private void hideSheet(Workbook workbook, String sheetName) {
		workbook.setSheetHidden(workbook.getSheetIndex(sheetName), true);
	}

	private String getColumnLetter(int columnIndex) {
		StringBuilder sb = new StringBuilder();
		int col = columnIndex;
		while (col >= 0) {
			sb.insert(0, (char) ('A' + col % 26));
			col = col / 26 - 1;
		}
		return sb.toString();
	}

	/**
	 * 下拉框配置.
	 */
	@Getter
	@RequiredArgsConstructor
	public static class DropdownConfig {

		/**
		 * 选项列表 ([label, value]).
		 */
		private final List<String[]> items;

		/**
		 * 下拉框列索引 (0-based).
		 */
		private final int dropdownColumnIndex;

		/**
		 * VLOOKUP 公式列索引 (0-based, 可选).
		 */
		private final Integer formulaColumnIndex;

		/**
		 * 简化构造: 无公式列.
		 * @param items 选项列表
		 * @param dropdownColumnIndex 下拉框列索引
		 */
		public DropdownConfig(List<String[]> items, int dropdownColumnIndex) {
			this(items, dropdownColumnIndex, null);
		}

	}

}
