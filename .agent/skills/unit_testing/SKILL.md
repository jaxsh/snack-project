---
name: unit_testing
description: Guidelines and standards for writing unit and integration tests, including JUnit 5 structure, naming conventions, and assertion rules.
---

# Testing Strategy
- **Framework**: JUnit 5 + Mockito + AssertJ + Spring Boot Test。

## A. Structure & Grouping
- **Nested Tests**: 优先使用 `@Nested` 内部类按场景或状态对测试方法进行分组 (e.g., `class CreateUser`, `class UpdateUser`)。
- **Helpers**: 测试专用的辅助类/方法应定义为 **Static Nested Classes** 或私有方法，保持测试类整洁。

## B. Naming Conventions
- **Prefix**: 测试方法名统一以 `should` 开头 (e.g., `shouldReturn404`).
- **Simplification**: 在 `@Nested` 上下文中，方法名可以简化。
    - *Example*: 在 `class CreateUser` 内部，使用 `shouldSucceed()` 而非 `shouldCreateUserSuccessfully()`。
- **Intent**: 方法名必须清晰表达预期行为，**做到“见名知意”，无需额外注释**。

## C. Assertions & Debugging
- **Merging Assertions**:
    - 使用 `AssertJ` 的 `SoftAssertions`。
    - 或使用 `MockMvc` 的 `andExpectAll(...)` 来合并多个 `andExpect` 调用，避免冗长的链式调用。
- **Debugging**: 当测试逻辑复杂或预期可能失败时，在 MockMvc 链中添加 `.andDo(print())` 以打印请求和响应详情。

## D. Documentation in Tests
- **No Redundancy**: 测试方法名清晰时，**严禁**添加注释。
- **Scenario Docs**: 可以在 `@Nested` 类上添加 Javadoc 说明测试场景。
- **Complex Logic**: 仅在测试逻辑极其复杂（涉及特殊的 Mock 设置或时序）时添加简短注释。

## E. Test Maintenance (Refactoring Rule)
- **Priority**: 当生产代码发生变更（重构或逻辑调整）且**代码逻辑本身正确**时，**必须修改测试用例**以适配新的行为。
- **Anti-Pattern**: **严禁**为了让旧测试通过而回退正确的生产代码修改。测试必须跟随代码演进，而不是阻碍改进。

## F. Code Quality & Compliance (Strict)
- **Import Order (Checkstyle - Spring Java Format)**:
    - 遵循 `SpringImportOrder`, 分组规则如下 (**组内连续排列，无空行；组间用空行分隔**):
        1.  `java.*` / `javax.*` (标准库)
        2.  **空行**
        3.  `com.*`, `lombok.*`, `org.jax.*` (第三方库和项目包，**按字母顺序连续排列**)
        4.  **空行**
        5.  `org.springframework.*` (Spring 框架)
    - **Anti-Pattern**: 严禁在 `com`/`lombok`/`org.jax` 之间插入空行，它们属于同一 import 组。
- **Class Layout (Checkstyle)**:
    - **InnerTypeLast**: 测试类中的所有 **Fields**, **Constructors**, **Helper Methods** 必须声明在 `@Nested` 内部测试类**之前**。
- **PMD Compliance (No Duplicate Literals)**:
    - **Strict Rule**: 严禁在测试代码中多次重复相同的字符串字面量 (e.g., `"value"`, `"length"` key names)。
    - **Strategy**: 优先使用 **Diversified Literals** (e.g., `"UserA"`, `"UserB"`) 在不同测试方法中分散字面量，自然规避重复阈值。
    - **Note**: 仅当必须使用完全相同的 Config Key 时，才允许定义 Private Static Constants。**严禁**为简单的测试数据定义 Helper Methods。
- **Static Constants Usage**:
    - **Allowed**: API URL 路径、重要的全局配置 Key。
    - **Discouraged**: 仅在单测内部使用的临时测试数据 (Test Data)，应通过 Parameterized Helpers 或局部变量解决。
