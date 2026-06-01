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

package org.jax.snack.oauth.biz.converter;

import org.jax.snack.framework.mybatisplus.converter.BasePageConvert;
import org.jax.snack.framework.utils.mapstruct.BaseDtoConvert;
import org.jax.snack.framework.utils.mapstruct.BaseMapStructConfig;
import org.jax.snack.oauth.api.dto.OAuthRegisteredClientDTO;
import org.jax.snack.oauth.api.vo.OAuthRegisteredClientVO;
import org.jax.snack.oauth.biz.entity.OAuthRegisteredClient;
import org.mapstruct.Mapper;

/**
 * OAuth2 客户端转换器.
 *
 * @author Jax Jiang
 */
@Mapper(config = BaseMapStructConfig.class)
public interface OAuthRegisteredClientConverter
		extends BaseDtoConvert<OAuthRegisteredClientDTO, OAuthRegisteredClient, OAuthRegisteredClientVO>,
		BasePageConvert<OAuthRegisteredClient, OAuthRegisteredClientVO> {

}
