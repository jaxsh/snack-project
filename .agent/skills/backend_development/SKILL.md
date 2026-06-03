---
name: backend_development
description: Comprehensive guide for backend development, covering API Design, Layering, and Implementation Patterns (CRUD).
---

# Backend Development Guide

## 1. API Design & Validation (Strict Compliance)

### A. Naming Conventions
- **Entity**: `FeatureName` (e.g., `SysUser`) - Table mapping. **MUST** extend `BaseEntity` and use `Lombok` specific annotations.
- **DTO**: `FeatureNameDTO` (e.g., `SysUserDTO`) - Input data.
- **VO**: `FeatureNameVO` (e.g., `SysUserVO`) - Output view.
- **Controller**: `FeatureNameController`.
- **Service**: `FeatureNameService` (Interface) -> `FeatureNameServiceImpl` (Impl).
- **Repository**: `FeatureNameRepository` (Interface).

### B. Validation Rules
- **No Message Attributes**: **Prohibited** `@NotBlank(message="...")`. Use global exception handling.
- **String Length**: `String` fields **MUST** have `@Size(max = X)`.
- **Validation Groups**:
    - **Create**: Use `@NotNull`/`@NotBlank` with `groups = Create.class`.
    - **Update**: Use `@Size` without groups to prevent empty strings if field is present.
    - **Default**: standard checks like `@Email`, `@Pattern` apply to both.

## 2. Module Distribution (Project Layout)
- **`*-api` Module** (Interface & Data Contracts):
    - DTO (`org.jax.snack.module.api.dto`)
    - VO (`org.jax.snack.module.api.vo`)
    - Service Interfaces (`org.jax.snack.module.api.service`)
    - Constants/Enums shared with other modules.
- **`*-biz` Module** (Implementation):
    - Controller (`org.jax.snack.module.biz.controller`)
    - Service Implementation (`org.jax.snack.module.biz.service.impl`)
    - Entity (`org.jax.snack.module.biz.entity`)
    - Repository (`org.jax.snack.module.biz.repository`)
    - Mapper (`org.jax.snack.module.biz.mapper`)
    - Converter (`org.jax.snack.module.biz.converter`)

## 3. Architecture & Layering

**Strict Data Flow**:
`Controller (DTO/VO)` ➡ `Service (DTO/Entity)` ➡ `Repository (Entity)` ➡ `Mapper (Entity)` ➡ `Database`

### A. Controller Layer
- **Responsibility**: HTTP handling, Validation, DTO/VO processing.
- **Rules**:
    - **Never** return Entity.
    - Use `QueryCondition` for complex queries. **Always** use `QueryCondition.builder()...build()` for internal construction.

### B. Service Layer
- **Responsibility**: Business Logic, Transactions, Conversion.
- **Rules**:
    - **Never** use `LambdaQueryWrapper`.
    - **Always** return VO or DTO (or void).
    - **Standard Flows** (Inheritance Pattern):
        - **Create**:
            1. Validate Business Logic (e.g., `repository.existsByDsl`).
            2. Convert DTO -> Entity.
            3. `repository.save(entity)`.
        - **Update**:
            1. Check Existence (`repository.findById` or throw).
            2. Convert DTO -> Entity.
            3. `repository.update(entity)`.
        - **Delete**:
            1. Build `WhereCondition` using `WhereCondition.builder().in(Fields.id, ids).build()`.
            2. Call `repository.deleteByDsl`.
        - **Query**:
            1. Call `repository.queryPageByDsl(condition)`.
            2. Convert to PageResult<VO> via Converter.
            3. *Tip*: Use `toBuilder()` to modify or extend existing conditions.

### C. Repository Layer
- **Responsibility**: DSL construction, Database interaction.
- **Implementation**: Extend `BaseRepository<Entity, Long>`.

### E. Entity Layer
- **Inheritance**: `extends BaseEntity`.
- **Annotations**:
    - `@Getter`, `@Setter` (No `@Data`).
    - `@FieldNameConstants` (For type-safe QueryWrappers).
    - `@TableName("table_name")`.
- **Fields Configuration**:
    - **MUST** include a `Fields` inner class extending `BaseEntity.Fields` to enable inherited field constants.
    ```java
    @FieldNameConstants
    public class SysUser extends BaseEntity {
        // ... fields ...

        public static final class Fields extends BaseEntity.Fields {}
    }
    ```

### F. Converter Layer (MapStruct)
- **Configuration**: `@Mapper(config = BaseMapStructConfig.class)`.
- **Inheritance**:
    - `BaseDtoConvert<DTO, Entity, VO>`
    - `BasePageConvert<Entity, VO>`

### G. Enum Layer
- **Interface**: `implements BaseEnum<Integer>` (or String).
- **Annotations**: `@Getter`, `@RequiredArgsConstructor`.
- **Structure**:
    - Fields: `code` and `name`.
    - Static Helper: `of(code)` calling `BaseEnum.fromCode`.

## 4. DDL/DML Standards (Liquibase YAML)

### A. File Structure
- **DDL Scripts**: `src/main/resources/db/changelog/ddl/*.yaml`
- **DML Scripts**: `src/main/resources/db/changelog/dml/*.yaml`
- **Master File**: `src/main/resources/db/changelog/db.changelog-master.yaml`
- **File Header Comment**: `# table_name 表说明` (Required).
- **Author**: Module name (e.g., `upms`).

### B. Field Type Mapping

| Semantic         | DB Type               | Java Type            | Examples              |
|:-----------------|:----------------------|:---------------------|:----------------------|
| Enabled/Disabled | `tinyint(1)`          | `Integer`            | `status`, `enabled`   |
| Yes/No Boolean   | `tinyint(1)`          | `Integer`            | `locked`, `read_flag` |
| Success/Failure  | `tinyint(1)`          | `Integer`            | `status` (log tables) |
| Enum Type        | `tinyint` / `varchar` | `Integer` / `String` | `type`, `method`      |

### C. Remarks Format
- **Pattern**: `"field_desc(0:value0, 1:value1)"`
- **Punctuation**: English `()`, `:`, `,` only.
- **Examples**:
    - `"状态(0:禁用, 1:启用)"`
    - `"锁定标志(0:未锁定, 1:已锁定)"`

### D. DML Rules
- **Prohibited**: `valueBoolean: true/false`.
- **Required**: `valueNumeric: 1/0`.

### E. Optional Fields
- `version` (Optimistic Lock): Add only when concurrency control is needed.

## 5. Entity Field Standards

### A. Status Field Type
- **Prohibited**: `Boolean`, `boolean`.
- **Required**: `Integer`.

### B. Enum Value Assignment
- **Prohibited**: Magic numbers `0`, `1`.
- **Required**: `Enum.CONSTANT.getCode()`.

### C. Common Enums (`org.jax.snack.framework.core.enums`)

| Enum            | Purpose          | Constants                   |
|:----------------|:-----------------|:----------------------------|
| `Status`        | Enabled/Disabled | `DISABLED(0)`, `ENABLED(1)` |
| `YesNoStatus`   | Yes/No Boolean   | `NO(0)`, `YES(1)`           |
| `SuccessStatus` | Success/Failure  | `FAIL(0)`, `SUCCESS(1)`     |

### D. Code Examples
```java
// ✅ Correct
public void correctUsage(SysUser user) {
    user.setEnabled(Status.ENABLED.getCode());
    user.setLocked(YesNoStatus.NO.getCode());
    if (Objects.equals(user.getLocked(), YesNoStatus.YES.getCode())) {
        throw new BusinessException(ErrorCode.USER_LOCKED);
    }
}

// ❌ Wrong
public void wrongUsage(SysUser user) {
    user.setEnabled(1);
    user.setLocked(false);  // Type error
    if (user.getLocked() == 1) {
        user.doSomething();
    }
}
```

### E. Converter Label Mapping
```java
public interface SysUserConverter {
    @AfterMapping
    default void afterToVO(Entity entity, @MappingTarget VO vo) {
        if (entity.getStatus() != null) {
            vo.setStatusLabel(BaseEnum.getNameByCode(Status.class, entity.getStatus()));
        }
    }
}
```
