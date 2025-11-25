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
 * MDC Bean 后置处理器配置类.
 * <p>
 * 专门负责注册与 MDC 相关的 {@link org.springframework.beans.factory.config.BeanPostProcessor}.
 * 独立配置类的目的是为了隔离生命周期，避免因过早实例化 BeanPostProcessor 而导致的循环依赖或 AOP 失效问题.
 *
 * @author Jax Jiang
 */
@Configuration(proxyBeanMethods = false)
public class MdcBeanPostProcessorConfiguration {

	/**
	 * 注册定时任务调度器的 MDC 配置器.
	 * <p>
	 * 该 Bean 是一个 {@link org.springframework.beans.factory.config.BeanPostProcessor}， 必须通过
	 * static 方法注册，以防止触发过早的 Bean 初始化.
	 * @return {@link MdcSchedulingConfigurer} 实例
	 */
	@Bean
	@ConditionalOnClass(ThreadPoolTaskScheduler.class)
	public static MdcSchedulingConfigurer mdcSchedulingConfigurer() {
		return new MdcSchedulingConfigurer();
	}

}
