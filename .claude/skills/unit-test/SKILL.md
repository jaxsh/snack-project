---
name: unit-test
description: Standards for writing unit and integration tests using JUnit 5, Mockito, and AssertJ. Use when writing or reviewing test code.
---

# Unit Testing Standards

Framework: JUnit 5 + Mockito + AssertJ + Spring Boot Test

## 1. Structure

Use `@Nested` to group scenarios and states:

```java
class SysUserControllerTest {

    @Nested
    class CreateUser {
        @Test
        void shouldSucceed() { ... }

        @Test
        void shouldFailWhenUsernameExists() { ... }
    }

    @Nested
    class UpdateUser {
        @Test
        void shouldSucceed() { ... }
    }
}
```

- Helper classes / methods → define as **static nested classes** or private methods
- Fields, constructors, helper methods must be declared **before** `@Nested` classes (Checkstyle: InnerTypeLast)

## 2. Naming

- Methods start with `should` (e.g., `shouldReturn404WhenNotFound`)
- Inside a `@Nested` context, simplify: `shouldSucceed()` not `shouldCreateUserSuccessfully()`
- Name must express expected behavior — no extra comments needed if the name is clear

## 3. Assertions

Merge multiple assertions with `SoftAssertions` or `andExpectAll`:

```java
// MockMvc — use andExpectAll, not chained andExpect
mockMvc.perform(post("/users").contentType(APPLICATION_JSON).content(body))
        .andDo(print())   // add when debugging complex scenarios
        .andExpectAll(
            status().isOk(),
            jsonPath("$.code").value(0),
            jsonPath("$.data.username").value("testUser")
        );

// Service/Unit — use SoftAssertions
SoftAssertions soft = new SoftAssertions();
soft.assertThat(result.getUsername()).isEqualTo("testUser");
soft.assertThat(result.getStatus()).isEqualTo(Status.ENABLED.getCode());
soft.assertAll();
```

Add `.andDo(print())` only when debugging — remove before committing.

## 4. Comments in Tests

- **No comments** when the method name already describes the scenario
- `@Nested` class may have Javadoc to describe the scenario group
- Add inline comments only for non-obvious Mock setup or timing-sensitive logic

## 5. Test Maintenance Rule

When production code changes and logic is still correct:
- **Must** update test cases to match the new behavior
- **Forbidden**: modifying production code to make old tests pass when those tests are wrong

## 6. Import Order (Spring Java Format — strict)

Groups in order, **continuous within group, blank line between groups**:

```java
import java.util.List;
import java.util.Map;
                                    // blank line
import com.example.Something;
import lombok.RequiredArgsConstructor;
import org.jax.snack.upms.api.dto.SysUserDTO;
                                    // blank line
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
```

- `java.*` / `javax.*` → Group 1
- `com.*`, `lombok.*`, `org.jax.*` → Group 2 (continuous, alphabetical, **no blank lines between them**)
- `org.springframework.*` → Group 3

**Forbidden**: blank lines between `com`, `lombok`, `org.jax` imports — they belong to the same group.

## 7. PMD: No Duplicate Literals

Forbidden: same string literal repeated across multiple test methods.

```java
// Wrong — "username" repeated
void test1() { assertThat(result.get("username")).isEqualTo("Alice"); }
void test2() { assertThat(result.get("username")).isEqualTo("Bob"); }

// Correct — diversify literals naturally
void test1() { assertThat(result.get("username")).isEqualTo("Alice"); }
void test2() { assertThat(result.get("email")).isEqualTo("bob@example.com"); }
```

Private static constants are allowed only for config keys reused across multiple tests.
Forbidden: constants for simple test data values.

## 8. Static Constants

Allowed:
- API URL paths (e.g., `private static final String URL = "/api/users";`)
- Config keys used across multiple tests

Not recommended:
- Temporary test data used only within a single method — use local variables instead

## 9. Field Injection in Tests

Allowed (unlike production code):
```java
@Autowired
private MockMvc mockMvc;

@MockitoBean
private SysUserService userService;
```

## 10. Verification

```
./gradlew :snack-module:snack-<module>:snack-<module>-biz:test
./gradlew build    # includes Checkstyle + PMD
```
