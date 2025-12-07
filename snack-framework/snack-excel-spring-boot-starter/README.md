# Snack Excel Spring Boot Starter

基于 [FastExcel](https://github.com/alibaba/easyexcel) 的 Excel/CSV 导入导出 Spring Boot Starter，提供简洁的 Builder API 和自动配置支持。

---

## 📦 核心功能

- ✅ **Builder 模式**: 链式调用配置导入导出参数
- ✅ **自动校验**: 支持 Jakarta Validation (JSR-303) 注解校验
- ✅ **批量处理**: 支持大文件分批读取，可配置批次大小
- **错误收集**: 支持快速失败或收集所有错误后统一抛出
- ✅ **时区支持**: 自动处理 ZonedDateTime 与 Web 请求时区的转换
- ✅ **样式定制**: 支持自定义 Excel 样式和列合并
- ✅ **CSV 支持**: 完整的 CSV 导入导出功能
- ✅ **可选依赖**: Validation 和 Web 依赖均为可选，按需引入

---

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.jax.snack.framework:snack-excel-spring-boot-starter'

    // 可选: 如需导入时进行 Bean Validation 校验
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // 可选: 如需使用 ResponseHelper 进行 HTTP 响应
    implementation 'org.springframework.boot:spring-boot-starter-web'
}
```

### 2. Excel 导出示例

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final ExcelWriteService excelWriteService;

    @GetMapping("/users/export")
    public void export(HttpServletResponse response) throws IOException {
        List<UserVO> users = userService.getAllUsers();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ExcelWriteBuilder.create(excelWriteService, buffer, users, UserVO.class)
            .sheetName("用户列表")
            .execute();

        ResponseHelper.downloadExcel(response, buffer, "users");
    }
}
```

### 3. Excel 导入示例

```java
@PostMapping("/users/import")
public void importUsers(@RequestParam("file") MultipartFile file) throws Exception {
    ExcelReadBuilder.create(excelReadService, file.getInputStream(), UserDTO.class)
        .batchSize(1000)
        .failFast(false)
        .execute((dataList) -> {
            List<User> users = dataList.stream()
                .map(converter::toEntity)
                .toList();
            userRepository.saveAll(users);
        });
}
```

---

## ⚙️ 配置

### application.yml

```yaml
snack:
  excel:
    read:
      batch-size: 500         # 批次大小，默认 500
      fail-fast: false        # 快速失败模式，默认 false
  csv:
    write:
      delimiter: COMMA        # 分隔符: COMMA, SEMICOLON, TAB
      quote: DOUBLE_QUOTE     # 引号: DOUBLE_QUOTE, SINGLE_QUOTE
      record-separator: CRLF  # 行分隔符: CRLF, LF
      null-string: ""         # null 值显示
```

---

## 📚 高级用法

### 1. 自定义 Excel 样式

```java
// 参数: 行高, 列宽, 是否锁定表头
ExcelWriteBuilder.create(excelWriteService, buffer, users, UserVO.class)
    .style(ExcelStyleFactory.create((short) 50, (short) 10, true))
    .execute();
```

### 2. 列合并

```java
// 合并第 2、3 列 (索引从 0 开始)
ExcelWriteBuilder.create(excelWriteService, buffer, data, DictDataVO.class)
    .mergeColumns(1, 2)
    .execute();
```

### 3. 动态表头

```java
// 自定义列名映射
ExcelWriteBuilder.create(excelWriteService, buffer, data, UserVO.class)
    .headers(Map.of(
        "name", "姓名",
        "age", "年龄"
    ))
    .execute();
```

### 4. 业务校验

```java
// 除 JSR-303 校验外，还可添加自定义业务校验
ExcelReadBuilder.create(excelReadService, inputStream, UserDTO.class)
    .businessValidator(user -> {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw BatchValidationException.forCollecting()
                .addError(new ExcelFieldError("username", "Username already exists", 0));
        }
    })
    .execute(userService::batchSave);
```

### 5. CSV 导出

```java
CsvWriteBuilder.create(excelWriteService, buffer, users, UserVO.class)
    .delimiter(CsvDelimiter.COMMA)
    .execute();
```

---

## 📝 实体类示例

### Excel 导出实体

```java
@Getter
@Setter
public class UserVO {

    @ExcelProperty(value = "用户ID", index = 0)
    private Long id;

    @ExcelProperty(value = "用户名", index = 1)
    private String username;

    @ExcelProperty(value = "年龄", index = 2)
    private Integer age;

    @ExcelProperty(value = "创建时间", index = 3)
    private ZonedDateTime createdAt;  // 自动转换时区
}
```

### Excel 导入实体

```java
@Getter
@Setter
public class UserDTO {

    @ExcelProperty(value = "用户名", index = 0)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @ExcelProperty(value = "年龄", index = 1)
    @Min(value = 18, message = "年龄不能小于18")
    private Integer age;

    @ExcelProperty(value = "邮箱", index = 2)
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

---

## ⚠️ 异常处理

`BatchValidationException` 包含所有校验错误：

```java
@ExceptionHandler(BatchValidationException.class)
public ResponseEntity<ErrorResponse> handleBatchValidation(BatchValidationException ex) {
    List<FieldError> errors = ex.getFieldErrors().stream()
        .map(e -> new FieldError(
            e.getFieldName(),
            e.getMessage(),
            e.getRowIndex()
        ))
        .toList();
    return ResponseEntity.badRequest().body(new ErrorResponse(errors));
}
```

---

## 🔧 依赖说明

### 核心依赖 (必需)

- cn.idev.excel:fastexcel:1.3.0
- org.springframework.boot:spring-boot-autoconfigure

### 可选依赖

| 依赖                                               | 用途                 | 缺失时的行为             |
|--------------------------------------------------|--------------------|--------------------| | jakarta.validation:jakarta.validation-api        | Bean Validation 校验 | 跳过 JSR-303 校验      |
| org.springframework.boot:spring-boot-starter-web | HTTP 响应辅助          | ResponseHelper 不可用 |

---

## 📄 License

Apache License 2.0
