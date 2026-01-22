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

package org.jax.snack.upms.biz.repository.impl;

import org.jax.snack.framework.mybatisplus.repository.AbstractRepository;
import org.jax.snack.upms.biz.entity.SysUserOrg;
import org.jax.snack.upms.biz.mapper.SysUserOrgMapper;
import org.jax.snack.upms.biz.repository.SysUserOrgRepository;

import org.springframework.stereotype.Repository;

/**
 * 用户-组织关联 Repository 实现.
 *
 * @author Jax Jiang
 */
@Repository
public class SysUserOrgRepositoryImpl extends AbstractRepository<SysUserOrg, Long, SysUserOrgMapper>
		implements SysUserOrgRepository {

}
