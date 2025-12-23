# snack-webtest-spring-boot-starter

Web 层集成测试工具模块, 提供 MockMvc 测试基类和便捷的断言 Matcher.

## 快速开始

### 1. 添加依赖

```groovy
testImplementation project(':snack-framework:snack-webtest-spring-boot-starter')
```

### 2. 配置测试类

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional  // 测试后自动回滚
@Sql(scripts = "/sql/test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class MyControllerTests extends MockMvcTestSupport {
    // ...
}
```

### 3. 编写测试

```java
class MyControllerTests extends MockMvcTestSupport {

    @Test
    void shouldCreateSuccess() throws Exception {
        MyDTO dto = new MyDTO();
        dto.setName("test");

        postJson("/api/my-entity", dto)
            .andDo(print())
            .andExpect(status().isOk())
            .andExpectAll(ApiResponseMatchers.isSuccess());
    }

}
```

## 核心组件

### MockMvcTestSupport

测试基类, 提供预配置的 HTTP 请求方法:

| 方法                                            | 说明            |
|-----------------------------------------------|---------------|
| `getJson(url)`                                | GET 请求        |
| `getJson(urlTemplate, uriVariables...)`       | 带路径参数的 GET    |
| `postJson(url, body)`                         | POST 请求       |
| `putJson(url, body)`                          | PUT 请求        |
| `putJson(urlTemplate, body, uriVariables...)` | 带路径参数的 PUT    |
| `deleteJson(url)`                             | DELETE 请求     |
| `deleteJson(urlTemplate, uriVariables...)`    | 带路径参数的 DELETE |

### ApiResponseMatchers

统一响应结构断言 (针对 `ApiResponse`):

```java
class ApiResponseExamples {
    void examples() throws Exception {
        // 验证成功响应
        mockMvc.perform(get("/api/test"))
            .andExpectAll(ApiResponseMatchers.isSuccess());

        // 验证错误响应
        mockMvc.perform(get("/api/test"))
            .andExpect(ApiResponseMatchers.isError("2001"));

        // 验证 data 字段值
        mockMvc.perform(get("/api/test"))
            .andExpect(ApiResponseMatchers.data(".name", "test"));

        // 验证 data 为数组
        mockMvc.perform(get("/api/test"))
            .andExpect(ApiResponseMatchers.dataIsArray());
    }
}
```

### PageResultMatchers

分页响应断言 (针对 `PageResult`):

```java
class PageResultExamples {
    void examples() throws Exception {
        // 验证总记录数
        mockMvc.perform(get("/api/test"))
            .andExpect(PageResultMatchers.totalIs(10));

        // 验证结果非空
        mockMvc.perform(get("/api/test"))
            .andExpect(PageResultMatchers.isNotEmpty());

        // 验证结果为空
        mockMvc.perform(get("/api/test"))
            .andExpect(PageResultMatchers.isEmpty());

        // 验证记录字段值 (索引从 0 开始)
        mockMvc.perform(get("/api/test"))
            .andExpect(PageResultMatchers.record(0, ".id", 1))
            .andExpect(PageResultMatchers.record(0, ".name", "test"));
    }
}
```

### ExceptionMatchers

异常响应断言:

```java
class ExceptionExamples {
    void examples() throws Exception {
        // 验证错误码
        mockMvc.perform(get("/api/test"))
            .andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID));

        // 验证字段校验错误
        mockMvc.perform(get("/api/test"))
            .andExpect(ExceptionMatchers.fieldHasError("name"));

        // 验证字段错误消息
        mockMvc.perform(get("/api/test"))
            .andExpectAll(ExceptionMatchers.fieldError("name", "不能为空"));
    }
}
```

## 完整示例

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/sql/test_data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class SysDictTypeControllerTests extends MockMvcTestSupport {

    private static final String BASE_URL = "/api/upms/dict-types";
    private static final String ID_PATH = "/{id}";

    @Nested
    class CreateDictType {

        @Test
        void shouldCreateAndVerifyData() throws Exception {
            String dictType = "test_" + System.currentTimeMillis();
            SysDictTypeDTO dto = new SysDictTypeDTO();
            dto.setDictName("测试");
            dto.setDictType(dictType);

            postJson(BASE_URL, dto)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpectAll(ApiResponseMatchers.isSuccess());

            // 查询验证
            QueryCondition condition = new QueryCondition();
            condition.setWhere(Map.of("dictType", Map.of("_eq", dictType)));
            postJson(BASE_URL + "/query", condition)
                .andExpect(status().isOk())
                .andExpect(PageResultMatchers.totalIs(1));
        }

        @Test
        void shouldFailWhenDictTypeBlank() throws Exception {
            SysDictTypeDTO dto = new SysDictTypeDTO();
            dto.setDictName("测试");

            postJson(BASE_URL, dto)
                .andExpect(status().isBadRequest())
                .andExpect(ExceptionMatchers.code(ErrorCode.PARAM_INVALID))
                .andExpect(ExceptionMatchers.fieldHasError("dictType"));
        }
    }

    @Nested
    class GetById {

        @Test
        void shouldReturnData() throws Exception {
            getJson(BASE_URL + ID_PATH, 1L)
                .andExpect(status().isOk())
                .andExpectAll(ApiResponseMatchers.isSuccess())
                .andExpect(PageResultMatchers.record(0, ".id", 1));
        }

        @Test
        void shouldReturnEmptyWhenNotFound() throws Exception {
            getJson(BASE_URL + ID_PATH, 99999L)
                .andExpect(status().isOk())
                .andExpect(PageResultMatchers.isEmpty());
        }
    }

    @Nested
    class DeleteDictType {

        @Test
        void shouldDeleteAndVerifyRemoved() throws Exception {
            // 删除
            deleteJson(BASE_URL + ID_PATH, 2L)
                .andExpect(status().isOk())
                .andExpectAll(ApiResponseMatchers.isSuccess());

            // 验证已删除
            getJson(BASE_URL + ID_PATH, 2L)
                .andExpect(PageResultMatchers.isEmpty());
        }
    }
}
```

## 注意事项

1. **避免静态导入**: Spring Checkstyle 不允许静态导入自定义 Matcher, 请使用类名调用
2. **提取常量**: PMD 要求避免重复字面量, 建议将常用字符串提取为常量
3. **事务回滚**: 使用 `@Transactional` 确保测试后数据自动回滚
