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

package org.jax.snack.oauth.biz.repository.impl;

import org.jax.snack.framework.mybatisplus.repository.AbstractRepository;
import org.jax.snack.oauth.biz.entity.OAuth2User;
import org.jax.snack.oauth.biz.mapper.OAuth2UserMapper;
import org.jax.snack.oauth.biz.repository.OAuth2UserRepository;

import org.springframework.stereotype.Repository;

/**
 * OAuth2 用户仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
public class OAuth2UserRepositoryImpl extends AbstractRepository<OAuth2User, Long, OAuth2UserMapper>
		implements OAuth2UserRepository {

}
