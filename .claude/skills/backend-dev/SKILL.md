---
name: backend-dev
description: Backend implementation patterns for CRUD operations, validation groups, converter mapping, and DDL standards. Use when implementing a new feature or entity.
---

# Backend Development Patterns

> Core naming, module structure, entity/service/repository rules are in project-rules.md.
> This skill covers implementation flows and patterns not found there.

## 1. Validation Groups

```java
// DTO validation pattern
public class SysUserDTO {
    @NotBlank(groups = Create.class)          // Required on create only
    @Size(max = 50)                           // Applied on both (no groups = default)
    private String username;

    @NotBlank(groups = Create.class)
    @Size(max = 100)
    private String password;

    @Email                                    // Applied on both (default group)
    @Size(max = 100)
    private String email;
}
```

Rules:
- `@NotNull` / `@NotBlank` → always add `groups = Create.class`
- `@Size` → **no groups** (prevents empty string on update if field is present)
- `@Email`, `@Pattern` → **no groups** (apply to both create and update)
- **Forbidden**: `@NotBlank(message="...")` — use global exception handling

Controller usage:
```java
// Create
public ResponseEntity<Void> create(@Validated(Create.class) @RequestBody SysUserDTO dto) {}

// Update
public ResponseEntity<Void> update(@Validated @RequestBody SysUserDTO dto) {}
```

## 2. Service CRUD Flows

### Create
```java
public void create(SysUserDTO dto) {
    // 1. Business validation
    if (repository.existsByDsl(QueryCondition.builder()
            .eq(SysUser.Fields.username, dto.getUsername()).build())) {
        throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
    }
    // 2. Convert DTO -> Entity
    SysUser entity = converter.toEntity(dto);
    // 3. Save
    repository.save(entity);
}
```

### Update
```java
public void update(Long id, SysUserDTO dto) {
    // 1. Check existence
    SysUser entity = repository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    // 2. Convert DTO -> Entity (converter merges into existing entity)
    converter.updateEntity(dto, entity);
    // 3. Update
    repository.update(entity);
}
```

### Delete
```java
public void delete(List<Long> ids) {
    WhereCondition condition = WhereCondition.builder()
            .in(SysUser.Fields.id, ids)
            .build();
    repository.deleteByDsl(condition);
}
```

### Query (paged)
```java
public PageResult<SysUserVO> queryPage(QueryCondition condition) {
    // Use toBuilder() to extend conditions if needed
    return repository.queryPageByDsl(condition)
            .convert(converter::toVO);
}
```

## 3. Enum Structure

```java
@Getter
@RequiredArgsConstructor
public enum UserType implements BaseEnum<Integer> {

    ADMIN(1, "管理员"),
    NORMAL(2, "普通用户");

    private final Integer code;
    private final String name;

    public static UserType of(Integer code) {
        return BaseEnum.fromCode(UserType.class, code);
    }
}
```

## 4. Converter Label Mapping

```java
@Mapper(config = BaseMapStructConfig.class)
public interface SysUserConverter extends BaseDtoConvert<SysUserDTO, SysUser, SysUserVO>,
        BasePageConvert<SysUser, SysUserVO> {

    @AfterMapping
    default void afterToVO(SysUser entity, @MappingTarget SysUserVO vo) {
        if (entity.getStatus() != null) {
            vo.setStatusLabel(BaseEnum.getNameByCode(Status.class, entity.getStatus()));
        }
        if (entity.getType() != null) {
            vo.setTypeLabel(BaseEnum.getNameByCode(UserType.class, entity.getType()));
        }
    }
}
```

## 5. DDL Field Type Mapping

| Semantic         | DB Type               | Java Type            | Example field    |
|:-----------------|:----------------------|:---------------------|:-----------------|
| Enabled/Disabled | `tinyint(1)`          | `Integer`            | `status`         |
| Yes/No           | `tinyint(1)`          | `Integer`            | `locked`         |
| Success/Failure  | `tinyint(1)`          | `Integer`            | `status` (logs)  |
| Enum type        | `tinyint` / `varchar` | `Integer` / `String` | `type`, `method` |

Remarks format: `"字段描述(0:值0, 1:值1)"` — English punctuation `()`, `:`, `,` only.

DML: use `valueNumeric: 1` / `valueNumeric: 0`. Forbidden: `valueBoolean: true/false`.

## 6. Verification

After implementing a feature, run in order:

```
./gradlew :snack-module:snack-<module>:snack-<module>-biz:compileJava
./gradlew :snack-module:snack-<module>:snack-<module>-biz:test
./gradlew format
./gradlew build
```
