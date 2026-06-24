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

package org.jax.snack.upms.controller;

import com.jayway.jsonpath.JsonPath;
import org.jax.snack.framework.core.api.query.QueryCondition;
import org.jax.snack.framework.webtest.matcher.ApiResponseMatchers;
import org.jax.snack.upms.UpmsIntegrationTests;
import org.jax.snack.upms.api.dto.SysUserDTO;
import org.jax.snack.upms.api.vo.SysUserVO;
import org.jax.snack.upms.biz.client.OAuth2UserClient;
import org.jax.snack.upms.biz.entity.SysUser;
import org.jax.snack.upms.biz.repository.SysFileRepository;
import org.jax.snack.upms.biz.service.impl.SysUserServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件管理集成测试.
 *
 * @author Jax Jiang
 */
@SpringBootTest(properties = { "snack.file.storage-path=build/uploads",
		"snack.file.allowed-extensions=jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,zip,rar,md,csv,json" })
class SysFileControllerTests extends UpmsIntegrationTests {

	private static final String API_FILES = "/api/upms/files";

	private static final String API_FILES_ID = API_FILES + "/{id}";

	private static final String API_FILES_DOWNLOAD = API_FILES + "/{id}/download";

	private static final String PARAM_FILE = "file";

	private static final String JSON_PATH_ID = "$.data.id";

	@Autowired
	private SysUserServiceImpl userService;

	@Autowired
	private SysFileRepository fileRepository;

	@MockitoBean
	private OAuth2UserClient oAuth2UserClient;

	@Nested
	class UploadFile {

		@Test
		void shouldUploadSuccessfully() throws Exception {
			MockMultipartFile file = new MockMultipartFile(PARAM_FILE, "test.txt", "text/plain",
					"hello world".getBytes());

			mockMvc.perform(multipart(API_FILES).file(file).with(defaultJwt()))
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.originalName").value("test.txt"))
				.andExpect(jsonPath("$.data.contentType").value("text/plain"))
				.andExpect(jsonPath("$.data.fileSize").value(11))
				.andExpect(jsonPath("$.data.url").isNotEmpty());
		}

		@Test
		void shouldDeduplicateWhenUploadingSameFile() throws Exception {
			MockMultipartFile file1 = new MockMultipartFile(PARAM_FILE, "test1.md", "text/markdown",
					"same content".getBytes());
			MockMultipartFile file2 = new MockMultipartFile(PARAM_FILE, "test2.md", "text/markdown",
					"same content".getBytes());

			mockMvc.perform(multipart(API_FILES).file(file1).with(defaultJwt())).andExpect(status().isOk());

			mockMvc.perform(multipart(API_FILES).file(file2).with(defaultJwt())).andExpect(status().isOk());
		}

	}

	@Nested
	class GetFileInfo {

		@Test
		void shouldReturnFileInfo() throws Exception {
			MockMultipartFile file = new MockMultipartFile(PARAM_FILE, "info.csv", "text/csv", "hello info".getBytes());
			MvcResult result = mockMvc.perform(multipart(API_FILES).file(file).with(defaultJwt())).andReturn();

			Long id = Long.valueOf(JsonPath.read(result.getResponse().getContentAsString(), JSON_PATH_ID).toString());

			mockMvc.perform(get(API_FILES_ID, id).with(defaultJwt()))
				.andExpect(status().isOk())
				.andExpectAll(ApiResponseMatchers.isSuccess())
				.andExpect(jsonPath("$.data.originalName").value("info.csv"))
				.andExpect(jsonPath(JSON_PATH_ID).value(id));
		}

	}

	@Nested
	class DownloadFile {

		@Test
		void shouldDownloadWithOriginalName() throws Exception {
			String content = "download content";
			MockMultipartFile file = new MockMultipartFile(PARAM_FILE, "download.pdf", "application/pdf",
					content.getBytes());
			MvcResult result = mockMvc.perform(multipart(API_FILES).file(file).with(defaultJwt())).andReturn();

			Long id = Long.valueOf(JsonPath.read(result.getResponse().getContentAsString(), JSON_PATH_ID).toString());

			mockMvc.perform(get(API_FILES_DOWNLOAD, id).with(defaultJwt()))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition", containsString("filename*=UTF-8''download.pdf")))
				.andExpect(content().string(content));
		}

	}

	@Nested
	class DeleteFile {

		@Test
		void shouldDeleteFileRecord() throws Exception {
			MockMultipartFile file = new MockMultipartFile(PARAM_FILE, "delete_rec.json", "application/json",
					"to delete".getBytes());
			MvcResult result = mockMvc.perform(multipart(API_FILES).file(file).with(defaultJwt())).andReturn();

			Long id = Long.valueOf(JsonPath.read(result.getResponse().getContentAsString(), JSON_PATH_ID).toString());

			mockMvc.perform(delete(API_FILES_ID, id).with(defaultJwt())).andExpect(status().isOk());

			mockMvc.perform(get(API_FILES_ID, id).with(defaultJwt())).andExpect(status().is(422));
		}

	}

	@Nested
	class LifecycleVerification {

		@Test
		void shouldManageFileLifecycle() throws Exception {
			MockMultipartFile fileA = new MockMultipartFile(PARAM_FILE, "avatar_a.png", "image/png",
					"content_a".getBytes());
			MvcResult resultA = mockMvc.perform(multipart(API_FILES).file(fileA).with(defaultJwt()))
				.andExpect(status().isOk())
				.andReturn();

			String urlA = JsonPath.read(resultA.getResponse().getContentAsString(), "$.data.url").toString();

			SysUserDTO userDto = new SysUserDTO();
			userDto.setUsername("lifecycle_mb_user");
			userDto.setNickname("Mobile User");
			userDto.setAvatar(JsonNullable.of(urlA));

			SysFileControllerTests.this.userService.create(userDto);
			QueryCondition userCondition = QueryCondition.builder()
				.eq(SysUser.Fields.username, "lifecycle_mb_user")
				.build();
			SysUserVO user = SysFileControllerTests.this.userService.queryByDsl(userCondition).getRecords().get(0);
			Long userId = user.getId();
			assertThat(user.getAvatar()).isEqualTo(urlA);

			MockMultipartFile fileB = new MockMultipartFile(PARAM_FILE, "avatar_b.png", "image/png",
					"content_b".getBytes());
			MvcResult resultB = mockMvc.perform(multipart(API_FILES).file(fileB).with(defaultJwt())).andReturn();
			Long fileIdB = Long
				.valueOf(JsonPath.read(resultB.getResponse().getContentAsString(), JSON_PATH_ID).toString());
			String urlB = JsonPath.read(resultB.getResponse().getContentAsString(), "$.data.url").toString();

			SysUserDTO updateDto = new SysUserDTO();
			updateDto.setAvatar(JsonNullable.of(urlB));
			SysFileControllerTests.this.userService.update(userId, updateDto);

			QueryCondition byIdCondition = QueryCondition.builder().eq(SysUser.Fields.id, userId).build();
			user = SysFileControllerTests.this.userService.queryByDsl(byIdCondition).getRecords().get(0);
			assertThat(user.getAvatar()).isEqualTo(urlB);

			assertThat(SysFileControllerTests.this.fileRepository.findById(fileIdB)).isPresent();
		}

	}

}
