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

import java.time.ZonedDateTime;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.jax.snack.framework.mybatisplus.context.UserContextProvider;
import org.jax.snack.framework.mybatisplus.manager.SystemFieldManager;

import org.springframework.stereotype.Component;

/**
 * 审计字段自动填充处理器.
 * <p>
 * 在插入或更新时自动填充创建人、更新人、创建时间、更新时间等审计字段.
 *
 * @author Jax Jiang
 */
@Component
@RequiredArgsConstructor
public class AuditMetaObjectHandler implements MetaObjectHandler {

	private final UserContextProvider userContextProvider;

	/**
	 * 插入时填充审计字段.
	 * <p>
	 * 自动填充创建人、创建时间、更新人、更新时间.
	 * @param metaObject 元对象
	 */
	@Override
	public void insertFill(MetaObject metaObject) {
		String currentUser = this.userContextProvider.getCurrentUsername();
		ZonedDateTime now = ZonedDateTime.now();

		this.strictInsertFill(metaObject, SystemFieldManager.FIELD_CREATE_BY, String.class, currentUser);
		this.strictInsertFill(metaObject, SystemFieldManager.FIELD_UPDATE_BY, String.class, currentUser);
		this.strictInsertFill(metaObject, SystemFieldManager.FIELD_CREATE_TIME, ZonedDateTime.class, now);
		this.strictInsertFill(metaObject, SystemFieldManager.FIELD_UPDATE_TIME, ZonedDateTime.class, now);
	}

	/**
	 * 更新时填充审计字段.
	 * <p>
	 * 自动填充更新人、更新时间.
	 * @param metaObject 元对象
	 */
	@Override
	public void updateFill(MetaObject metaObject) {
		this.strictUpdateFill(metaObject, SystemFieldManager.FIELD_UPDATE_BY, String.class,
				this.userContextProvider.getCurrentUsername());
		this.strictUpdateFill(metaObject, SystemFieldManager.FIELD_UPDATE_TIME, ZonedDateTime.class,
				ZonedDateTime.now());
	}

}
