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

import java.util.Iterator;
import java.util.function.Predicate;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.pattern.parser.Node;
import ch.qos.logback.core.pattern.parser.Parser;
import ch.qos.logback.core.spi.ScanException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Spring Boot 应用的 Logback 自动配置器. 该组件在应用启动后，动态地将 MDC 信息（如全链路追踪 ID）注入到所有 使用
 * PatternLayoutEncoder 的 Appender 的日志格式中. 行为由 MdcProperties 类高度控制.
 *
 * @author Jax Jiang
 * @since 2025-06-02
 */
@Slf4j
@RequiredArgsConstructor
public class MdcLogbackConfigurer {

	private final MdcProperties properties;

	/**
	 * 监听 ApplicationReadyEvent 事件，以确保所有日志配置都已加载完毕. 这是执行日志格式修改的安全时机.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void configureLogback() {
		if (!this.properties.isEnabled()) {
			return;
		}
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		for (Logger logger : context.getLoggerList()) {
			Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
			while (appenderIterator.hasNext()) {
				Appender<ILoggingEvent> appender = appenderIterator.next();
				try {
					this.processAppender(appender, context);
				}
				catch (ScanException ex) {
					log.warn("MDC Configurer: Failed to process appender [{}] due to a pattern parsing error.",
							appender.getName(), ex);
				}
			}
		}
	}

	/**
	 * 处理单个 Appender，如果符合条件，则修改其日志格式.
	 * @param appender 要处理的 Appender.
	 * @param context 当前的 LoggerContext.
	 */
	private void processAppender(Appender<ILoggingEvent> appender, LoggerContext context) throws ScanException {
		// 阶段1: 前置条件检查，确保 Appender 和其组件是我们可以处理的类型.
		if (!(appender instanceof OutputStreamAppender<?> osAppender)) {
			return;
		}
		if (!(osAppender.getEncoder() instanceof PatternLayoutEncoder encoder)) {
			return;
		}
		String currentPattern = encoder.getPattern();
		if (currentPattern == null) {
			return;
		}
		if (!(encoder.getLayout() instanceof PatternLayout layout)) {
			return;
		}

		// 阶段2: 智能幂等性检查。解析当前模式，如果已存在任何 MDC 转换符，则跳过.
		Parser<Object> parser = new Parser<>(currentPattern);
		parser.setContext(context);
		Node topNode = parser.parse();
		Predicate<Node> mdcExistsPredicate = LogbackNodeUtils.getNodePredicateForConverter(MDCConverter.class, layout);
		if (LogbackNodeUtils.findNode(topNode, mdcExistsPredicate) != null) {
			log.debug("MDC injection skipped for appender [{}]: Pattern already contains an MDC converter.",
					appender.getName());
			return;
		}

		// 阶段3: 执行注入逻辑.
		Class<?> targetClass = this.properties.getTargetConverter();
		String traceIdContent = this.properties.getTraceIdPattern();

		// 安全检查：不允许将注入目标设置为 MDCConverter 本身.
		if (MDCConverter.class.equals(targetClass)) {
			log.debug("MDC injection skipped for appender [{}]: Target converter cannot be MDCConverter itself.",
					appender.getName());
			return;
		}

		// 如果未配置目标，执行回退策略：在头部添加.
		if (targetClass == null) {
			log.info(
					"MDC Configurer for appender [{}]: No target converter configured. Prepending traceId to the beginning as a fallback.",
					appender.getName());
			String newPattern = traceIdContent + currentPattern;
			this.updateEncoder(appender, encoder, newPattern);
			return;
		}

		// 准备注入操作，如果注入失败，则执行回退策略.
		Predicate<Node> injectionTargetPredicate = LogbackNodeUtils.getNodePredicateForConverter(targetClass, layout);
		Parser<Object> injectParser = new Parser<>(traceIdContent);
		injectParser.setContext(context);
		Node traceIdNodeHead = injectParser.parse();
		String newPattern;
		if (LogbackNodeUtils.recursiveInject(topNode, traceIdNodeHead, injectionTargetPredicate)) {
			newPattern = LogbackNodeUtils.rebuildPattern(topNode);
		}
		else {
			log.warn(
					"MDC Configurer for appender [{}]: Target converter '{}' was not found. Prepending traceId as a fallback.",
					appender.getName(), targetClass.getSimpleName());
			newPattern = traceIdContent + currentPattern;
		}
		this.updateEncoder(appender, encoder, newPattern);
	}

	/**
	 * 封装更新 Encoder 的原子操作，并记录详细的变更日志.
	 * @param appender 被修改的 Appender，用于日志记录.
	 * @param encoder 要更新的 Encoder.
	 * @param newPattern 新的日志格式.
	 */
	private void updateEncoder(Appender<ILoggingEvent> appender, PatternLayoutEncoder encoder, String newPattern) {
		log.info("Updating log pattern for appender [{}]:\n  Old: {}\n  New: {}", appender.getName(),
				encoder.getPattern(), newPattern);
		encoder.stop();
		encoder.setPattern(newPattern);
		encoder.start();
	}

}
