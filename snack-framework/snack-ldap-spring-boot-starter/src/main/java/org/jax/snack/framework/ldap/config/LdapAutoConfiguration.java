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

package org.jax.snack.framework.ldap.config;

import org.jax.snack.framework.ldap.convert.FileTimeToZonedDateTimeConverter;
import org.jax.snack.framework.ldap.convert.ObjectGUIDToUUIDConverter;
import org.jax.snack.framework.ldap.convert.ZonedDateTimeToFileTimeConverter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;

/**
 * LDAP自动配置类, 用于配置LDAP相关的组件.
 * <p>
 * 主要功能: 1. 配置LdapTemplate 2. 配置LdapContextSource 3. 支持自定义LDAP配置
 * <p>
 * 工作流程: 1. 检查LDAP依赖是否存在 2. 创建LdapContextSource实例 3. 配置LDAP连接参数 4. 创建LdapTemplate实例
 * <p>
 * 配置特性: 1. 支持自定义LDAP服务器配置 2. 支持自定义认证方式 3. 支持自定义连接池配置
 *
 * @author Jax Jiang
 */
@AutoConfiguration
public class LdapAutoConfiguration {

	/**
	 * 创建对象目录映射器. 配置LDAP对象与Java对象之间的转换规则.
	 * @return 配置好的对象目录映射器
	 */
	@Bean
	public ObjectDirectoryMapper objectDirectoryMapper() {
		DefaultObjectDirectoryMapper mapper = new DefaultObjectDirectoryMapper();
		GenericConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new ObjectGUIDToUUIDConverter());
		conversionService.addConverter(new ZonedDateTimeToFileTimeConverter());
		conversionService.addConverter(new FileTimeToZonedDateTimeConverter());
		mapper.setConversionService(conversionService);
		return mapper;
	}

}
