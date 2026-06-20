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

package org.jax.snack.upms.biz.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.jax.snack.framework.oauth2.client.security.OidcScopeGrantedAuthoritiesMapper;
import org.jax.snack.upms.api.service.SysUserService;
import org.jax.snack.upms.api.vo.SysResourceVO;
import org.jspecify.annotations.NullMarked;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * UPMS 权限映射器.
 * <p>
 * 登录成功后, 根据用户信息从数据库加载细粒度权限 (SysResource).
 *
 * @author Jax Jiang
 */
@Slf4j
@Component
public class UpmsGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

	private final SysUserService sysUserService;

	private final GrantedAuthoritiesMapper delegate;

	public UpmsGrantedAuthoritiesMapper(SysUserService sysUserService,
			ObjectProvider<OidcScopeGrantedAuthoritiesMapper> delegateProvider) {
		this.sysUserService = sysUserService;
		this.delegate = delegateProvider.getIfAvailable(OidcScopeGrantedAuthoritiesMapper::new);
	}

	@NullMarked
	@Override
	public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
		Collection<? extends GrantedAuthority> baseAuthorities = this.delegate.mapAuthorities(authorities);
		Set<GrantedAuthority> mapped = new HashSet<>(baseAuthorities);

		String username = extractUsername(authorities);
		if (username != null) {
			List<String> roles = this.sysUserService.getUserRoles(username);
			for (String roleCode : roles) {
				if (StringUtils.hasText(roleCode)) {
					mapped.add(new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase(Locale.ROOT)));
				}
			}

			List<SysResourceVO> resources = this.sysUserService.getUserResources(username);
			for (SysResourceVO resource : resources) {
				if (Integer.valueOf(2).equals(resource.getType()) && StringUtils.hasText(resource.getMethod())
						&& StringUtils.hasText(resource.getPath())) {
					mapped.add(new SimpleGrantedAuthority(
							resource.getMethod().toUpperCase(Locale.ROOT) + ":" + resource.getPath()));
				}
			}
			log.debug("Loaded {} permissions for user '{}'", resources.size(), username);

		}

		return mapped;
	}

	private String extractUsername(Collection<? extends GrantedAuthority> authorities) {
		for (GrantedAuthority authority : authorities) {
			if (authority instanceof OidcUserAuthority oidcAuth) {
				return oidcAuth.getIdToken().getSubject();
			}
			else if (authority instanceof OAuth2UserAuthority oauth2Auth) {
				Map<String, Object> attributes = oauth2Auth.getAttributes();
				Object sub = attributes.get("sub");
				if (sub != null) {
					return sub.toString();
				}
				Object name = attributes.get("name");
				if (name != null) {
					return name.toString();
				}
			}
		}
		return null;
	}

}
