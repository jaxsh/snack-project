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

package org.jax.snack.upms.biz.job;

import java.time.ZonedDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.upms.biz.entity.SysMessageLog;
import org.jax.snack.upms.biz.entity.SysNotification;
import org.jax.snack.upms.biz.repository.SysMessageLogRepository;
import org.jax.snack.upms.biz.repository.SysNotificationRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息清理定时任务.
 * <p>
 * 清理过期的消息发送日志和已读站内信.
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupJob implements Job {

	private static final String PARAM_LOG_RETAIN_DAYS = "logRetainDays";

	private static final String PARAM_NOTIFICATION_RETAIN_DAYS = "notificationRetainDays";

	private static final int DEFAULT_LOG_RETAIN_DAYS = 30;

	private static final int DEFAULT_NOTIFICATION_RETAIN_DAYS = 30;

	private final SysMessageLogRepository messageLogRepository;

	private final SysNotificationRepository notificationRepository;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void execute(JobExecutionContext context) {
		int logRetainDays = context.getMergedJobDataMap().containsKey(PARAM_LOG_RETAIN_DAYS)
				? context.getMergedJobDataMap().getIntValue(PARAM_LOG_RETAIN_DAYS) : DEFAULT_LOG_RETAIN_DAYS;

		int notificationRetainDays = context.getMergedJobDataMap().containsKey(PARAM_NOTIFICATION_RETAIN_DAYS)
				? context.getMergedJobDataMap().getIntValue(PARAM_NOTIFICATION_RETAIN_DAYS)
				: DEFAULT_NOTIFICATION_RETAIN_DAYS;

		int logDeleted = cleanupMessageLogs(logRetainDays);
		int notificationDeleted = cleanupNotifications(notificationRetainDays);

		log.info("Message cleanup finished: deleted {} logs, {} notifications", logDeleted, notificationDeleted);
	}

	/**
	 * 清理过期消息日志.
	 * @param retainDays 保留天数
	 * @return 删除数量
	 */
	private int cleanupMessageLogs(int retainDays) {
		ZonedDateTime threshold = ZonedDateTime.now().minusDays(retainDays);
		WhereCondition condition = WhereCondition.builder().lt(SysMessageLog.Fields.createTime, threshold).build();
		int deleted = this.messageLogRepository.deleteByDsl(condition);
		log.debug("Deleted {} message logs older than {} days", deleted, retainDays);
		return deleted;
	}

	/**
	 * 清理已读且过期的站内信.
	 * @param retainDays 保留天数
	 * @return 删除数量
	 */
	private int cleanupNotifications(int retainDays) {
		ZonedDateTime threshold = ZonedDateTime.now().minusDays(retainDays);
		WhereCondition condition = WhereCondition.builder()
			.eq(SysNotification.Fields.readFlag, "1")
			.lt(SysNotification.Fields.readTime, threshold)
			.build();
		int deleted = this.notificationRepository.deleteByDsl(condition);
		log.debug("Deleted {} read notifications older than {} days", deleted, retainDays);
		return deleted;
	}

}
