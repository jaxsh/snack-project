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
 * Logback 动态配置器.
 * <p>
 * 监听 {@link ApplicationReadyEvent} 事件, 在应用启动后动态修改 Logback 的 Appender 配置, 将 Trace ID
 * 格式注入到日志模式中.
 *
 * @author Jax Jiang
 */
@Slf4j
@RequiredArgsConstructor
public class MdcLogbackConfigurer {

	private final MdcProperties properties;

	/**
	 * 执行 Logback 配置修改.
	 * <p>
	 * 仅在 MDC 功能开启时执行.
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
	 * 处理单个 Appender.
	 * <p>
	 * 检查 Appender 类型, 解析当前日志模式, 并决定如何注入 Trace ID.
	 * @param appender 目标 Appender
	 * @param context Logger 上下文
	 * @throws ScanException 如果日志模式解析失败
	 */
	private void processAppender(Appender<ILoggingEvent> appender, LoggerContext context) throws ScanException {
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

		Parser<Object> parser = new Parser<>(currentPattern);
		parser.setContext(context);
		Node topNode = parser.parse();
		Predicate<Node> mdcExistsPredicate = LogbackNodeUtils.getNodePredicateForConverter(MDCConverter.class, layout);
		if (LogbackNodeUtils.findNode(topNode, mdcExistsPredicate) != null) {
			log.debug("MDC injection skipped for appender [{}]: Pattern already contains an MDC converter.",
					appender.getName());
			return;
		}

		Class<?> targetClass = this.properties.getTargetConverter();
		String traceIdContent = this.properties.getTraceIdPattern();

		if (MDCConverter.class.equals(targetClass)) {
			log.debug("MDC injection skipped for appender [{}]: Target converter cannot be MDCConverter itself.",
					appender.getName());
			return;
		}

		if (targetClass == null) {
			log.info(
					"MDC Configurer for appender [{}]: No target converter configured. Prepending traceId to the beginning as a fallback.",
					appender.getName());
			String newPattern = traceIdContent + currentPattern;
			this.updateEncoder(appender, encoder, newPattern);
			return;
		}

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
	 * 更新 Encoder 的日志模式.
	 * <p>
	 * 停止 Encoder, 设置新模式, 然后重新启动.
	 * @param appender 关联的 Appender
	 * @param encoder 需要更新的 Encoder
	 * @param newPattern 新的日志模式字符串
	 */
	private void updateEncoder(Appender<ILoggingEvent> appender, PatternLayoutEncoder encoder, String newPattern) {
		log.info("Updating log pattern for appender [{}]:\n  Old: {}\n  New: {}", appender.getName(),
				encoder.getPattern(), newPattern);
		encoder.stop();
		encoder.setPattern(newPattern);
		encoder.start();
	}

}
