# snack-project Rules

## Tech Stack
- **Java**: target 17 syntax, JDK 21 compiler. Forbidden: Records Patterns, Pattern Matching for switch, Virtual Threads (Java 21 features).
- **Spring Boot**: 4.0.6
- **ORM**: MyBatis-Plus 3.5.x — use `QueryCondition` DSL. Never use `LambdaQueryWrapper`.
- **Migration**: Liquibase YAML only.
- **Mapping**: MapStruct with `@Mapper(config = BaseMapStructConfig.class)`.
- **Build**: Gradle Groovy DSL + buildSrc convention plugins. Single quotes only. No parentheses on method calls. No re-declaring dependencies already included by conventions (e.g., Lombok, Spring Boot Starter).

## Module Structure
Three-layer pattern per business module:
- `*-api`: DTOs, VOs, service interfaces, enums — package `org.jax.snack.<module>.api.*`
- `*-biz`: controllers, service impls, entities, repositories, mappers, converters — package `org.jax.snack.<module>.biz.*`
- `*-server`: Spring Boot entry point only

Data flow (one direction only):
`Controller (DTO/VO)` → `Service (DTO/Entity)` → `Repository (Entity)` → `Mapper` → `Database`

## Lombok Rules
- **Forbidden**: `@Data` on any class (Entity, DTO, VO, POJO).
- Replace with explicit: `@Getter` + `@Setter` + `@ToString` + `@RequiredArgsConstructor` (as needed).
- Use `@RequiredArgsConstructor` + `final` fields for constructor injection.
- Use `@Builder` for complex object construction.

## Entity Rules
- Must `extends BaseEntity`.
- Annotations: `@Getter`, `@Setter`, `@FieldNameConstants`, `@TableName("table_name")`.
- Must include an inner `Fields` class:
  ```java
  public static final class Fields extends BaseEntity.Fields {}
  ```
- Status fields: `Integer` only. Forbidden: `Boolean`, `boolean`.
- Enum values: always use `Enum.CONSTANT.getCode()`. Forbidden: magic numbers `0`, `1`.

## Common Enums (`org.jax.snack.framework.core.enums`)
| Enum            | Constants                   |
|-----------------|-----------------------------|
| `Status`        | `DISABLED(0)`, `ENABLED(1)` |
| `YesNoStatus`   | `NO(0)`, `YES(1)`           |
| `SuccessStatus` | `FAIL(0)`, `SUCCESS(1)`     |

## Service Layer Rules
- Never return Entity. Always return VO, DTO, or void.
- Never use `LambdaQueryWrapper`. Use `QueryCondition.builder()...build()`.

Standard implementation patterns (apply when writing or modifying service methods):

| Operation | Pattern                                                                                                                   |
|-----------|---------------------------------------------------------------------------------------------------------------------------|
| Create    | 1. business validation → 2. `converter.toEntity(dto)` → 3. `repository.save(entity)`                                      |
| Update    | 1. `repository.findById(id).orElseThrow(...)` → 2. `converter.updateEntity(dto, entity)` → 3. `repository.update(entity)` |
| Delete    | `WhereCondition.builder().in(Fields.id, ids).build()` → `repository.deleteByDsl(condition)`                               |
| Query     | `repository.queryPageByDsl(condition)` → `converter.toVO(...)`                                                            |

> For full code examples with validation groups and converter mapping, invoke `/backend-dev`.

## Repository Layer Rules
- Extend `BaseRepository<Entity, Long>`.

## Null / Empty Checks
Forbidden: `if (str != null && !str.isEmpty())` or `if (list != null && !list.isEmpty())`.
Required:
- String: `StringUtils.hasText()` or `StringUtils.hasLength()`
- Collection/Map: `CollectionUtils.isEmpty()`
- Object/Array: `ObjectUtils.isEmpty()`

## Import Order (Checkstyle enforced — cannot be auto-fixed, must be written correctly)

**Strict groups, blank line between groups, NO blank lines within a group:**

```java
// Group 1: java / javax
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
                                        // ← blank line
// Group 2: everything else — com.*, lombok.*, org.jax.* — alphabetical, CONTINUOUS
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.jax.snack.framework.core.enums.Status;
import org.jax.snack.upms.api.dto.SysUserDTO;
                                        // ← blank line
// Group 3: org.springframework.*
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
```

Rules:
- `java.*` / `javax.*` → Group 1
- `com.*`, `lombok.*`, `org.jax.*` → Group 2, **alphabetical order, no blank lines between them**
- `org.springframework.*` → Group 3
- Static imports go before Group 1 with their own blank line separator
- **Never** insert a blank line between `com.*`, `lombok.*`, and `org.jax.*` imports — they are one group

## Code Quality
- No `@Autowired` field injection in production code. Constructor injection only.
- No `@SuppressWarnings` of any kind.
- No FQCN in code body (e.g., `java.util.List`). Use imports.
- Log messages in English.
- Javadoc in Chinese. No trailing comments; comments go above the code they describe.

## DDL/DML (Liquibase YAML)
- DDL: `src/main/resources/db/changelog/ddl/*.yaml`
- DML: `src/main/resources/db/changelog/dml/*.yaml`
- Boolean columns: `tinyint(1)` in DB, `Integer` in Java. Forbidden: `valueBoolean: true/false`. Use `valueNumeric: 1/0`.
- Remarks format: `"字段描述(0:值0, 1:值1)"` — English punctuation only.

## Verification Sequence (run after every change)
```
./gradlew :module-path:compileJava   # syntax check
./gradlew :module-path:test          # business logic check
./gradlew format                     # code format
./gradlew build                      # full check (Checkstyle, PMD)
```
