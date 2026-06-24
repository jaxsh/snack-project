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

package org.jax.snack.framework.mybatisplus.repository;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.core.api.query.UpdateCondition;
import org.jax.snack.framework.core.api.query.WhereCondition;
import org.jax.snack.framework.mybatisplus.query.WrapperBuilder;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Repository 抽象基类.
 * <p>
 * 提供基于 MyBatis-Plus 的标准 CRUD 实现. 子类只需注入 Mapper 并指定实体类型即可获得完整功能.
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @param <M> Mapper 类型
 * @author Jax Jiang
 */
public abstract class AbstractRepository<T, ID extends Serializable, M extends BaseMapper<T>>
		implements BaseRepository<T, ID> {

	private M mapper;

	private final Class<T> entityClass;

	@SuppressWarnings("unchecked")
	public AbstractRepository() {
		Type superClass = getClass().getGenericSuperclass();
		if (superClass instanceof ParameterizedType) {
			Type[] typeArgs = ((ParameterizedType) superClass).getActualTypeArguments();
			this.entityClass = (Class<T>) typeArgs[0];
		}
		else {
			this.entityClass = null;
		}
	}

	/**
	 * Setter 注入 Mapper.
	 * @param mapper Mapper 实例
	 */
	@Autowired
	public void setMapper(M mapper) {
		this.mapper = mapper;
	}

	/**
	 * 获取 Mapper 实例 (供子类扩展使用).
	 * @return Mapper 实例
	 */
	protected M getMapper() {
		return this.mapper;
	}

	@Override
	public void save(T entity) {
		this.mapper.insert(entity);
	}

	@Override
	public void saveBatch(Collection<T> entities) {
		for (T entity : entities) {
			this.mapper.insert(entity);
		}
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(this.mapper.selectById(id));
	}

	@Override
	public void update(T entity) {
		this.mapper.updateById(entity);
	}

	@Override
	public void deleteById(ID id) {
		this.mapper.deleteById(id);
	}

	@Override
	public boolean existsByDsl(QueryCondition condition) {
		QueryWrapper<T> wrapper = WrapperBuilder.query(condition, this.entityClass);
		return this.mapper.exists(wrapper);
	}

	@Override
	public Page<T> queryPageByDsl(QueryCondition condition) {
		QueryWrapper<T> wrapper = WrapperBuilder.query(condition, this.entityClass);
		long current = (condition.getCurrent() != null) ? condition.getCurrent() : 1L;
		long size = (condition.getSize() != null) ? condition.getSize() : 10L;
		Page<T> page = new Page<>(current, size);
		return this.mapper.selectPage(page, wrapper);
	}

	@Override
	public List<T> queryListByDsl(QueryCondition condition) {
		QueryWrapper<T> wrapper = WrapperBuilder.query(condition, this.entityClass);
		return this.mapper.selectList(wrapper);
	}

	@Override
	public int updateByDsl(T entity, UpdateCondition condition) {
		UpdateWrapper<T> wrapper = WrapperBuilder.update(condition, this.entityClass);
		return this.mapper.update(entity, wrapper);
	}

	@Override
	public int deleteByDsl(WhereCondition condition) {
		QueryWrapper<T> wrapper = WrapperBuilder.where(condition, this.entityClass);
		return this.mapper.delete(wrapper);
	}

}
