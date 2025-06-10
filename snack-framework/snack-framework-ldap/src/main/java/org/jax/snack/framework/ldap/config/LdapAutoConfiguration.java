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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.odm.core.ObjectDirectoryMapper;
import org.springframework.ldap.odm.core.impl.DefaultObjectDirectoryMapper;

/**
 * LDAP自动配置类. 提供LDAP相关的配置, 包括对象目录映射器和LDAP模板的定制.
 *
 * @author Jax Jiang
 * @since 2025-05-30
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

	/**
	 * 定制LDAP模板. 配置LDAP模板使用自定义的对象目录映射器.
	 * @param ldapTemplate ldap模板
	 * @param objectDirectoryMapper 对象目录映射器
	 * @return 定制后的ldap模板
	 */
	@Bean
	@ConditionalOnBean(LdapTemplate.class)
	public LdapTemplate customizeLdapTemplate(LdapTemplate ldapTemplate, ObjectDirectoryMapper objectDirectoryMapper) {
		ldapTemplate.setObjectDirectoryMapper(objectDirectoryMapper);
		return ldapTemplate;
	}

}
