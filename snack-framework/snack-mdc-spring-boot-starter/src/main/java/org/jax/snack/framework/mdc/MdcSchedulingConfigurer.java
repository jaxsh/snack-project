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
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务调度器 MDC 配置器.
 * <p>
 * 实现 {@link BeanPostProcessor}，用于拦截并增强 Spring 的 {@link ThreadPoolTaskScheduler}.
 * <p>
 * <b>功能：</b> 通过动态代理（AOP）机制，拦截 {@code @Scheduled} 任务的提交过程，为每个定时任务执行前生成独立的 Trace ID，
 * 并在任务结束后清理，确保定时任务日志具备可追踪性.
 *
 * @author Jax Jiang
 */
@Slf4j
public class MdcSchedulingConfigurer implements BeanPostProcessor, ApplicationContextAware {

	private ApplicationContext applicationContext;

	/**
	 * 设置应用上下文.
	 * @param applicationContext Spring 应用上下文
	 * @throws BeansException 如果设置失败
	 */
	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * Bean 初始化后置处理.
	 * <p>
	 * 扫描 {@link ThreadPoolTaskScheduler} 类型的 Bean 并进行包装.
	 * @param bean Bean 实例
	 * @param beanName Bean 名称
	 * @return 可能被包装后的 Bean 实例
	 * @throws BeansException 如果处理失败
	 */
	@Override
	public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
		if (bean instanceof ThreadPoolTaskScheduler) {
			log.info("MDC Configurer: Found ThreadPoolTaskScheduler bean [{}]. Wrapping it to add MDC context.",
					beanName);
			return wrapTaskScheduler((ThreadPoolTaskScheduler) bean);
		}
		return bean;
	}

	/**
	 * 包装 TaskScheduler 以支持 MDC.
	 * <p>
	 * 使用 Spring AOP {@link ProxyFactory} 创建代理，拦截任务提交方法.
	 * @param scheduler 原始的任务调度器
	 * @return 代理后的调度器对象
	 */
	private Object wrapTaskScheduler(ThreadPoolTaskScheduler scheduler) {
		ProxyFactory proxyFactory = new ProxyFactory(scheduler);
		proxyFactory.addAdvice((org.aopalliance.intercept.MethodInterceptor) (invocation) -> {
			String methodName = invocation.getMethod().getName();
			Object[] args = invocation.getArguments();
			if (isTaskSubmissionMethod(methodName) && args.length > 0 && args[0] instanceof Runnable originalTask) {
				Runnable wrappedTask = () -> {
					// 动态获取 Bean 以避免循环依赖
					MdcProperties properties = this.applicationContext.getBean(MdcProperties.class);
					TraceIdGenerator traceIdGenerator = this.applicationContext.getBean(TraceIdGenerator.class);

					MDC.put(properties.getTraceIdKey(), traceIdGenerator.generate());
					try {
						originalTask.run();
					}
					finally {
						MDC.clear();
					}
				};

				Object[] newArgs = args.clone();
				newArgs[0] = wrappedTask;
				return invocation.getMethod().invoke(scheduler, newArgs);
			}
			return invocation.proceed();
		});
		return proxyFactory.getProxy();
	}

	/**
	 * 判断方法是否为任务提交方法.
	 * @param methodName 方法名
	 * @return 如果是 {@code schedule} 或 {@code submit} 开头则返回 true
	 */
	private boolean isTaskSubmissionMethod(String methodName) {
		return methodName.startsWith("schedule") || methodName.startsWith("submit");
	}

}
