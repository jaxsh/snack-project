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

package org.jax.snack.framework.excel.merge;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * Excel 列合并策略 - 相同数据自动合并单元格.
 * <p>
 * 性能优化： 1. 使用缓存机制避免 O(N²) 的重复搜索. 2. 使用倒序查找优化移除合并区域的性能 (O(1) in practice).
 *
 * @author Jax Jiang
 */
public class ColumnMergeStrategy extends AbstractMergeStrategy {

	private final int[] mergeColumnIndexes;

	/**
	 * 缓存每列最后的合并区域 (Key: 列索引, Value: 区域对象).
	 */
	private final Map<Integer, CellRangeAddress> lastMergedRegionCache = new HashMap<>();

	/**
	 * 记录上一次处理的行号，用于检测是否换行或非连续写入.
	 */
	private int lastRowIndex = -1;

	public ColumnMergeStrategy(int... mergeColumnIndexes) {
		if (mergeColumnIndexes == null || mergeColumnIndexes.length == 0) {
			throw new IllegalArgumentException("Merge column indexes cannot be empty");
		}
		this.mergeColumnIndexes = Arrays.copyOf(mergeColumnIndexes, mergeColumnIndexes.length);
		Arrays.sort(this.mergeColumnIndexes);
	}

	@Override
	protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
		if (relativeRowIndex == null || relativeRowIndex == 0) {
			return;
		}

		int currentRow = cell.getRowIndex();
		int currentCol = cell.getColumnIndex();

		if (currentRow > this.lastRowIndex + 1) {
			this.lastMergedRegionCache.clear();
		}
		this.lastRowIndex = currentRow;

		if (Arrays.binarySearch(this.mergeColumnIndexes, currentCol) >= 0) {
			mergeWithPreviousRow(sheet, currentRow, currentCol);
		}
	}

	private void mergeWithPreviousRow(Sheet sheet, int currentRow, int currentCol) {
		Object currentValue = getCellValue(sheet.getRow(currentRow).getCell(currentCol));
		Object previousValue = getCellValue(sheet.getRow(currentRow - 1).getCell(currentCol));

		if (!isSameValue(currentValue, previousValue)) {
			return;
		}

		CellRangeAddress cachedRegion = this.lastMergedRegionCache.get(currentCol);

		if (cachedRegion != null && cachedRegion.getLastRow() == currentRow - 1) {
			expandMergedRegion(sheet, cachedRegion, currentRow);
		}
		else {
			CellRangeAddress newRegion = new CellRangeAddress(currentRow - 1, currentRow, currentCol, currentCol);
			sheet.addMergedRegion(newRegion);
			this.lastMergedRegionCache.put(currentCol, newRegion);
		}
	}

	private void expandMergedRegion(Sheet sheet, CellRangeAddress region, int newLastRow) {
		List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
		for (int i = mergedRegions.size() - 1; i >= 0; i--) {
			CellRangeAddress existing = mergedRegions.get(i);
			if (existing.equals(region)) {
				sheet.removeMergedRegion(i);
				region.setLastRow(newLastRow);
				sheet.addMergedRegion(region);
				return;
			}
		}
	}

	private Object getCellValue(Cell cell) {
		if (cell == null) {
			return null;
		}
		return switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue();
			case NUMERIC -> cell.getNumericCellValue();
			case BOOLEAN -> cell.getBooleanCellValue();
			default -> null;
		};
	}

	private boolean isSameValue(Object value1, Object value2) {
		if (value1 == null && value2 == null) {
			return true;
		}
		if (value1 == null || value2 == null) {
			return false;
		}
		return value1.equals(value2);
	}

}
