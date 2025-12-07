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

package org.jax.snack.framework.excel.style;

import cn.idev.excel.write.metadata.style.WriteCellStyle;
import cn.idev.excel.write.metadata.style.WriteFont;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * Excel 样式工厂.
 *
 * @author Jax Jiang
 */
public final class ExcelStyleFactory {

	/**
	 * 默认样式 (蓝灰色, 9号字体, 带边框).
	 */
	public static final HorizontalCellStyleStrategy DEFAULT = create((short) 31, (short) 9, true);

	/**
	 * 简约样式 (无背景色, 9号字体, 无边框).
	 */
	public static final HorizontalCellStyleStrategy MINIMAL = create(null, (short) 9, false);

	private ExcelStyleFactory() {
	}

	/**
	 * 创建样式.
	 * @param colorIndex POI 颜色索引 (null 表示无背景色)
	 * @param fontSize 内容字体大小 (头部会自动 +1)
	 * @param withBorder 是否带边框
	 * @return HorizontalCellStyleStrategy
	 */
	public static HorizontalCellStyleStrategy create(Short colorIndex, short fontSize, boolean withBorder) {
		WriteCellStyle headCellStyle = new WriteCellStyle();
		headCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
		headCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		if (colorIndex != null) {
			headCellStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
			headCellStyle.setFillForegroundColor(colorIndex);
		}

		if (withBorder) {
			setBorderStyle(headCellStyle);
		}

		WriteFont headFont = new WriteFont();
		headFont.setFontHeightInPoints((short) (fontSize + 1));
		headFont.setBold(true);
		headCellStyle.setWriteFont(headFont);

		WriteCellStyle contentCellStyle = new WriteCellStyle();
		contentCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

		if (withBorder) {
			setBorderStyle(contentCellStyle);
		}

		WriteFont contentFont = new WriteFont();
		contentFont.setFontHeightInPoints(fontSize);
		contentCellStyle.setWriteFont(contentFont);

		return new HorizontalCellStyleStrategy(headCellStyle, contentCellStyle);
	}

	private static void setBorderStyle(WriteCellStyle style) {
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setTopBorderColor((short) 0);
		style.setBottomBorderColor((short) 0);
		style.setLeftBorderColor((short) 0);
		style.setRightBorderColor((short) 0);
	}

}
