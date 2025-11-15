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

package org.jax.snack.framework.mdc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 一个专门用于定义 MDC 相关 BeanPostProcessor 的配置类.
 *
 * @author Jax Jiang
 * @since 2025-07-05
 */
@Configuration(proxyBeanMethods = false)
public class MdcBeanPostProcessorConfiguration {

	/**
	 * 定义用于增强定时任务的 MdcSchedulingConfigurer.
	 * <p>
	 * 这是一个 {@link org.springframework.beans.factory.config.BeanPostProcessor}。
	 * 将其定义在一个独立的配置类中，并使用静态 @Bean 方法，是避免循环依赖和启动时序问题的最佳实践。
	 * @return {@link MdcSchedulingConfigurer} 的实例。
	 */
	@Bean
	@ConditionalOnClass(ThreadPoolTaskScheduler.class)
	public static MdcSchedulingConfigurer mdcSchedulingConfigurer() {
		// 创建一个没有任何依赖的实例
		return new MdcSchedulingConfigurer();
	}

}
