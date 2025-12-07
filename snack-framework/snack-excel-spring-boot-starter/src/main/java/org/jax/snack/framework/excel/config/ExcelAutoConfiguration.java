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

package org.jax.snack.framework.excel.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.jax.snack.framework.excel.ExcelBuilderFactory;
import org.jax.snack.framework.excel.ExcelReadService;
import org.jax.snack.framework.excel.ExcelWriteService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Excel 自动配置.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
@EnableConfigurationProperties({ ExcelProperties.class, CsvProperties.class })
public class ExcelAutoConfiguration {

	/**
	 * 创建 ExcelReadService.
	 * @param excelProperties Excel 配置
	 * @param validator JSR-303 校验器 (可选)
	 * @return ExcelReadService
	 */
	@Bean
	@ConditionalOnMissingBean
	public ExcelReadService excelReadService(ExcelProperties excelProperties,
			@Autowired(required = false) Validator validator) {
		return new ExcelReadService(excelProperties, validator);
	}

	/**
	 * 创建 ExcelWriteService.
	 * @param excelProperties Excel 配置
	 * @param csvProperties CSV 配置
	 * @return ExcelWriteService
	 */
	@Bean
	@ConditionalOnMissingBean
	public ExcelWriteService excelWriteService(ExcelProperties excelProperties, CsvProperties csvProperties) {
		return new ExcelWriteService(excelProperties, csvProperties);
	}

	/**
	 * 创建 ExcelBuilderFactory.
	 * @param excelReadService Excel 读取服务
	 * @param excelWriteService Excel 写入服务
	 * @return ExcelBuilderFactory
	 */
	@Bean
	@ConditionalOnMissingBean
	public ExcelBuilderFactory excelBuilderFactory(ExcelReadService excelReadService,
			ExcelWriteService excelWriteService) {
		return new ExcelBuilderFactory(excelReadService, excelWriteService);
	}

	/**
	 * 注册 JSR-303 校验器.
	 * @return Validator
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(Validator.class)
	public Validator validator() {
		return Validation.buildDefaultValidatorFactory().getValidator();
	}

}
