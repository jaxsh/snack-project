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

import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.mdc.generator.TraceIdGenerator;
import org.slf4j.MDC;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 一个 {@link BeanPostProcessor}, 用于自动为 Spring 的默认 TaskScheduler (bean name:
 * 'taskScheduler') 添加 MDC 上下文处理能力.
 * <p>
 * 它通过动态代理包装调度器, 为每个通过 {@code @Scheduled} 注解执行的定时任务, 在运行时生成一个唯一的 traceId, 从而实现对定时任务的日志追踪.
 *
 * @author Jax Jiang
 * @since 2025-06-15
 */
@Slf4j
public class MdcSchedulingConfigurer implements BeanPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 在每个 Bean 初始化完成之后被调用. 此方法用于识别并包装 Spring 默认的 {@link ThreadPoolTaskScheduler}.
	 * @param bean 刚刚初始化完成的 Bean 实例.
	 * @param beanName 该 Bean 在 Spring 容器中的名称.
	 * @return 返回原始 Bean, 或一个被包装过的代理对象.
	 */
	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		// 仅对 Spring 用于 @Scheduled 的默认调度器进行处理.
		if (bean instanceof ThreadPoolTaskScheduler) {
			log.info("MDC Configurer: Found ThreadPoolTaskScheduler bean [{}]. Wrapping it to add MDC context.",
					beanName);
			return wrapTaskScheduler((ThreadPoolTaskScheduler) bean);
		}
		return bean;
	}

	/**
	 * 使用 Spring AOP 的 ProxyFactory 来创建一个 TaskScheduler 的代理对象.
	 * @param scheduler 原始的、由 Spring 配置好的 ThreadPoolTaskScheduler 实例.
	 * @return 一个拦截了任务提交方法的代理对象.
	 */
	private Object wrapTaskScheduler(ThreadPoolTaskScheduler scheduler) {
		// 创建一个代理工厂, 目标是原始的 scheduler.
		ProxyFactory proxyFactory = new ProxyFactory(scheduler);
		// 添加一个方法拦截器, 这是代理的核心逻辑.
		proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) (invocation) -> {
			// 判断当前被调用的方法是否是我们关心的“任务提交”类方法.
			if (isTaskSubmissionMethod(invocation.getMethod().getName())) {
				// 从方法参数中获取原始的 Runnable 任务.
				Runnable originalTask = (Runnable) invocation.getArguments()[0];
				// 创建一个新的包装任务, 以实现 MDC 功能.
				Runnable wrappedTask = () -> {
					// 1. 在任务执行前, 使用通用的 traceId 生成器创建一个新的 traceId.
					MdcProperties properties = this.applicationContext.getBean(MdcProperties.class);
					TraceIdGenerator traceIdGenerator = this.applicationContext.getBean(TraceIdGenerator.class);

					MDC.put(properties.getTraceIdKey(), traceIdGenerator.generate());
					try {
						// 2. 执行真正的、原始的任务逻辑.
						originalTask.run();
					}
					finally {
						// 3. 无论任务成功还是失败, 在 finally 块中确保 MDC 被清理.
						MDC.clear();
					}
				};

				// 准备新的方法参数数组, 用我们包装过的 wrappedTask 替换原始任务.
				Object[] newArgs = invocation.getArguments().clone();
				newArgs[0] = wrappedTask;
				// 调用原始 scheduler 的方法, 但使用的是我们修改过的参数.
				return invocation.getMethod().invoke(scheduler, newArgs);
			}
			// 如果不是任务提交方法 (如 shutdown()), 直接让其正常执行.
			return invocation.proceed();
		});
		// 返回创建好的代理对象, Spring 容器中最终注册的就是这个代理.
		return proxyFactory.getProxy();
	}

	/**
	 * 辅助方法, 用于判断一个方法名是否属于任务提交类型.
	 * @param methodName 要检查的方法名.
	 * @return 如果方法名以 "schedule" 或 "submit" 开头, 则返回 true.
	 */
	private boolean isTaskSubmissionMethod(String methodName) {
		return methodName.startsWith("schedule") || methodName.startsWith("submit");
	}

}
