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

package org.jax.snack.framework.excel.converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import cn.idev.excel.converters.Converter;
import cn.idev.excel.enums.CellDataTypeEnum;
import cn.idev.excel.metadata.GlobalConfiguration;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.metadata.property.ExcelContentProperty;
import cn.idev.excel.util.DateUtils;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * ZonedDateTime String Converter (For CSV).
 * <p>
 * 专门用于 CSV 导出，将 ZonedDateTime 转换为格式化后的字符串。
 *
 * @author Jax Jiang
 */
public class ZonedDateTimeStringConverter implements Converter<ZonedDateTime> {

	@Override
	public Class<?> supportJavaTypeKey() {
		return ZonedDateTime.class;
	}

	@Override
	public CellDataTypeEnum supportExcelTypeKey() {
		return CellDataTypeEnum.STRING;
	}

	@Override
	public WriteCellData<?> convertToExcelData(ZonedDateTime value, ExcelContentProperty contentProperty,
			GlobalConfiguration globalConfiguration) {
		if (value == null) {
			return new WriteCellData<>("");
		}

		ZoneId targetZone = determineTargetZone();
		LocalDateTime localDateTime = value.withZoneSameInstant(targetZone).toLocalDateTime();

		String format = null;
		if (contentProperty != null && contentProperty.getDateTimeFormatProperty() != null) {
			format = contentProperty.getDateTimeFormatProperty().getFormat();
		}

		if (format == null) {
			format = DateUtils.defaultDateFormat;
		}

		String stringValue = DateTimeFormatter.ofPattern(format).format(localDateTime);
		return new WriteCellData<>(stringValue);
	}

	private ZoneId determineTargetZone() {
		try {
			if (isWebEnvironment()) {
				return LocaleContextHolder.getTimeZone().toZoneId();
			}
		}
		catch (IllegalStateException ignored) {
		}
		return ZoneOffset.UTC;
	}

	private boolean isWebEnvironment() {
		try {
			return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes;
		}
		catch (IllegalStateException ex) {
			return false;
		}
	}

}
