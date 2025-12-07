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

package org.jax.snack.framework.excel.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;

/**
 * HTTP 响应辅助工具.
 * <p>
 * 提供设置响应头和输出文件的通用方法.
 *
 * @author Jax Jiang
 */
public final class ResponseHelper {

	private static final String CONTENT_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private static final String CONTENT_TYPE_CSV = "text/csv";

	private ResponseHelper() {
	}

	/**
	 * 下载 Excel 文件.
	 * @param response HTTP 响应
	 * @param buffer 字节流缓冲区
	 * @param filename 文件名 (不含扩展名)
	 * @throws IOException IO 异常
	 */
	public static void downloadExcel(HttpServletResponse response, ByteArrayOutputStream buffer, String filename)
			throws IOException {
		download(response, buffer, filename + ".xlsx", CONTENT_TYPE_XLSX);
	}

	/**
	 * 下载 CSV 文件.
	 * @param response HTTP 响应
	 * @param buffer 字节流缓冲区
	 * @param filename 文件名 (不含扩展名)
	 * @throws IOException IO 异常
	 */
	public static void downloadCsv(HttpServletResponse response, ByteArrayOutputStream buffer, String filename)
			throws IOException {
		download(response, buffer, filename + ".csv", CONTENT_TYPE_CSV);
	}

	/**
	 * 下载文件.
	 * <p>
	 * 设置响应头并输出字节流到客户端.
	 * @param response HTTP 响应
	 * @param buffer 字节流缓冲区
	 * @param filename 文件名 (含扩展名)
	 * @param contentType 内容类型
	 * @throws IOException IO 异常
	 */
	private static void download(HttpServletResponse response, ByteArrayOutputStream buffer, String filename,
			String contentType) throws IOException {
		response.setContentType(contentType);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		ContentDisposition contentDisposition = ContentDisposition.attachment()
			.filename(filename, StandardCharsets.UTF_8)
			.build();
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

		buffer.writeTo(response.getOutputStream());
	}

}
