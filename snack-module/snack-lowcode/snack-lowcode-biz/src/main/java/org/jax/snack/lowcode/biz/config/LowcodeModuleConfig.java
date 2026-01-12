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

package org.jax.snack.lowcode.biz.config;

import org.mybatis.spring.annotation.MapperScan;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * LowCode 模块配置.
 * <p>
 * 配置组件扫描和 MyBatis Mapper 扫描.
 * <p>
 * 注: @MapperScan 是必须的, 因为当 LowCode 作为依赖被其他模块引入时, 主应用的自动扫描范围不包含 org.jax.snack.lowcode 包.
 *
 * @author Jax Jiang
 */
@Configuration
@ComponentScan("org.jax.snack.lowcode")
@MapperScan("org.jax.snack.lowcode.biz.mapper")
public class LowcodeModuleConfig {

}
