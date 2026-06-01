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

package org.jax.snack.oauth.biz.repository;

import org.jax.snack.framework.mybatisplus.repository.BaseRepository;
import org.jax.snack.oauth.biz.entity.OAuthRegisteredClient;

/**
 * OAuth2 客户端仓储接口.
 * <p>
 * 标准化 Entity 仓储，不再直接继承 RegisteredClientRepository 以避免 findById 冲突. 业务框架适配由 Adapter 实现.
 *
 * @author Jax Jiang
 */
public interface OAuthRegisteredClientRepository extends BaseRepository<OAuthRegisteredClient, String> {

}
