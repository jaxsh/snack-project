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

package org.jax.snack.framework.mybatisplus.config;

import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.MybatisMapWrapperFactory;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jax.snack.framework.mybatisplus.context.DefaultUserContextProvider;
import org.jax.snack.framework.mybatisplus.context.SecurityContextUserProvider;
import org.jax.snack.framework.mybatisplus.context.TableContextHolder;
import org.jax.snack.framework.mybatisplus.context.UserContextProvider;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;

/**
 * MyBatis-Plus 自动配置类.
 * <p>
 * 提供分页插件等常用配置, 简化业务模块使用.
 *
 * @author Jax Jiang
 */
@AutoConfiguration
@Import({ AuditMetaObjectHandler.class, SystemFieldManager.class })
public class MybatisPlusAutoConfiguration {

	/**
	 * 配置 MyBatis-Plus 拦截器.
	 * <p>
	 * 注册动态表名插件和分页插件 (单页最大 500 条).
	 * @return MyBatis-Plus 拦截器实例
	 */
	@Bean
	@ConditionalOnMissingBean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

		DynamicTableNameInnerInterceptor dynamicTableNameInnerInterceptor = new DynamicTableNameInnerInterceptor(
				(sql, tableName) -> {
					String dynamicName = TableContextHolder.getTableName();
					return (dynamicName != null) ? dynamicName : tableName;
				});
		interceptor.addInnerInterceptor(dynamicTableNameInnerInterceptor);

		PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
		paginationInterceptor.setMaxLimit(500L);
		paginationInterceptor.setOverflow(false);

		interceptor.addInnerInterceptor(paginationInterceptor);

		return interceptor;
	}

	/**
	 * 配置 MyBatis 对象包装器工厂.
	 * <p>
	 * 使用 {@link MybatisMapWrapperFactory} 支持 Map 结果集的下划线转驼峰等功能.
	 * @return 配置定制器
	 */
	@Bean
	@ConditionalOnMissingBean
	public ConfigurationCustomizer mybatisConfigurationCustomizer() {
		return (configuration) -> configuration.setObjectWrapperFactory(new MybatisMapWrapperFactory());
	}

	/**
	 * 配置基于 Spring Security 的用户上下文提供者.
	 * <p>
	 * 仅在 Spring Security 存在时生效.
	 * @return Spring Security 用户上下文提供者
	 */
	@Bean
	@ConditionalOnClass(Authentication.class)
	@ConditionalOnMissingBean
	public UserContextProvider securityContextUserProvider() {
		return new SecurityContextUserProvider();
	}

	/**
	 * 配置默认的用户上下文提供者.
	 * <p>
	 * 仅在没有其他 UserContextProvider 实现时生效, 始终返回 "system".
	 * @return 默认用户上下文提供者
	 */
	@Bean
	@ConditionalOnMissingBean
	public UserContextProvider defaultUserContextProvider() {
		return new DefaultUserContextProvider();
	}

}
