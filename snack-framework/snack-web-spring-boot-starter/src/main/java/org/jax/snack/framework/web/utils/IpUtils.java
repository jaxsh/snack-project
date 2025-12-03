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

package org.jax.snack.framework.web.utils;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

/**
 * IP 地址工具类.
 *
 * @author Jax Jiang
 */
public final class IpUtils {

	private static final String UNKNOWN = "unknown";

	private static final String COMMA = ",";

	private IpUtils() {
	}

	/**
	 * 获取客户端真实 IP 地址.
	 * <p>
	 * 依次尝试从 X-Forwarded-For 等请求头中获取, 如果获取失败则返回 RemoteAddr.
	 * @param request HttpServletRequest
	 * @return IP 地址
	 */
	public static String getIpAddr(HttpServletRequest request) {
		if (request == null) {
			return UNKNOWN;
		}

		String ip = request.getHeader("x-forwarded-for");
		if (!isValid(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (!isValid(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (!isValid(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (!isValid(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (!isValid(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (!isValid(ip)) {
			ip = request.getRemoteAddr();
		}

		return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : getMultistageReverseProxyIp(ip);
	}

	/**
	 * 校验 IP 是否有效.
	 * @param ip IP 地址
	 * @return 是否有效
	 */
	private static boolean isValid(String ip) {
		return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip);
	}

	/**
	 * 从多级反向代理中获得第一个非 unknown IP 地址.
	 * @param ip 可能包含多个 IP 的字符串
	 * @return 第一个 IP
	 */
	private static String getMultistageReverseProxyIp(String ip) {
		if (ip != null && ip.indexOf(COMMA) > 0) {
			final String[] ips = ip.trim().split(COMMA);
			for (String subIp : ips) {
				if (isValid(subIp)) {
					return subIp;
				}
			}
		}
		return ip;
	}

}
