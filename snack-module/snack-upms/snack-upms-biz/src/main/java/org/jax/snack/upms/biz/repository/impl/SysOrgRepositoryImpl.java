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

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.jax.snack.framework.mybatisplus.repository.AbstractRepository;
import org.jax.snack.upms.biz.entity.SysOrg;
import org.jax.snack.upms.biz.mapper.SysOrgMapper;
import org.jax.snack.upms.biz.repository.SysOrgRepository;

import org.springframework.stereotype.Repository;

/**
 * 组织机构仓储实现.
 *
 * @author Jax Jiang
 */
@Repository
public class SysOrgRepositoryImpl extends AbstractRepository<SysOrg, Long, SysOrgMapper> implements SysOrgRepository {

	@Override
	public void batchUpdateDescendants(String oldPrefix, String newPrefix, int levelDiff) {
		String ancestorsCol = SysOrg.Fields.ancestors;

		LambdaUpdateWrapper<SysOrg> wrapper = Wrappers.<SysOrg>lambdaUpdate()
			.likeRight(SysOrg::getAncestors, oldPrefix)
			.setSql(ancestorsCol + " = REPLACE(" + ancestorsCol + ", {0}, {1})", oldPrefix, newPrefix)
			.setIncrBy(SysOrg::getLevel, levelDiff);
		getMapper().update(wrapper);
	}

}
