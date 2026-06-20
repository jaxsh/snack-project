# Snack MyBatis-Plus Spring Boot Starter

基于 MyBatis-Plus 的增强型查询模块，提供 **GraphQL 风格的通用查询 API**，让前端可以灵活构建复杂查询条件。同时提供统一的 `Repository` 层契约，简化业务开发。

---

## 📦 核心功能

- ✅ **通用查询构建器**：支持静态方法调用，快速构建 `QueryWrapper` 和 `UpdateWrapper`。
- ✅ **22 种查询操作符**：涵盖 `_eq`, `_like`, `_in`, `_between` 等常见场景。
- ✅ **无限嵌套逻辑**：支持 `_and`, `_or`, `_not` 的递归组合。
- ✅ **字段选择与排序**：支持类似 GraphQL 的 `select` 指定字段、多字段 `orderBy` 及 `groupBy`/`having` 聚合。
- ✅ **Repository 集成**：提供 `BaseRepository` 契约，支持 DSL 分页、批量更新/删除。
- ✅ **原子更新**：支持 `incrBy` 和 `decrBy` 原子操作。
- ✅ **安全性**：通过类型安全的 Builder API 确保查询条件的结构化与完整性，有效防护 SQL 注入。

---

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.jax.snack.framework:snack-mybatisplus-spring-boot-starter'
}
```

### 2. Controller 示例 (静态调用模式)

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    @PostMapping("/query")
    public List<User> queryUsers(@RequestBody QueryCondition condition) {
        // 使用静态方法构建 Wrapper
        QueryWrapper<User> wrapper = WrapperBuilder.query(condition, User.class);
        return userMapper.selectList(wrapper);
    }
}
```

---

## 🏗️ Repository 层集成 (推荐)

项目提供了 `BaseRepository` 接口及 `AbstractRepository` 抽象类，建议在 Service 层通过 Repository 进行操作。

### 1. 定义 Repository
```java
public interface UserRepository extends BaseRepository<User, Long> {
}

@Repository
public class UserRepositoryImpl extends AbstractRepository<User, Long, UserMapper> implements UserRepository {
}
```

### 2. 使用 DSL 方法
```java
// 在 Service 中直接调用 Repository 方法
public void searchUsers(QueryCondition condition) {
    // 分页查询 (自动处理 QueryCondition 中的 current 和 size)
    Page<User> page = userRepository.queryPageByDsl(condition);

    // 列表查询
    List<User> list = userRepository.queryListByDsl(condition);

    // 批量删除 (使用 WhereCondition)
    userRepository.deleteByDsl(condition);
}
```

---

## 🔄 更新操作说明

模块通过单一方法 `int updateByDsl(T entity, UpdateCondition condition)` 提供 DSL 更新，与查询侧 `QueryCondition` 对称：

- **`entity`**：提供需更新的非 `null` 业务字段（由 MyBatis-Plus 原生写入），并触发审计字段 (`update_by`/`update_time`) 自动填充。
- **`condition`**：仅承载"需显式置 `null` 的列"与原子操作 —— `setNull(field)` 单字段置空、`setNulls(dto)` 由 DTO 的 JsonNullable 解包出需置空字段，以及 `incrBy`/`decrBy`、`where`。
- **不传的字段不更新**：实体中为 `null` 的字段不写入，未声明置空的列保持不变。

```java
public void updateExamples(SysUserDTO dto, Long id, WhereCondition where) {
    // 增量更新 + 清空某列：实体提供新值，setNull 指定要置 NULL 的列
    SysUser entity = converter.toEntity(dto);
    userRepository.updateByDsl(entity, UpdateCondition.builder()
        .setNull(SysUser.Fields.remark)
        .eq(SysUser.Fields.id, id)
        .build());

    // 由 DTO 解包置空字段（JsonNullable 中显式传 null 的字段）
    userRepository.updateByDsl(converter.toEntity(dto),
        UpdateCondition.builder().setNulls(dto).eq(SysUser.Fields.id, id).build());

    // 纯字段更新：非 null 值设在实体上，setNull 清空
    SysUser patch = new SysUser();
    patch.setStatus(Status.DISABLED.getCode());
    userRepository.updateByDsl(patch, UpdateCondition.builder()
        .setNull(SysUser.Fields.lockUntil)
        .where(where)
        .build());
}
```

---

## 📡 前端查询指南

### 基础 JSON 结构
```json
{
  "select": ["id", "username", "email"],
  "current": 1,
  "size": 10,
  "where": {
    "status": { "_eq": "ACTIVE" }
  },
  "orderBy": [
    { "field": "createTime", "direction": "desc" }
  ],
  "groupBy": ["department"],
  "having": "COUNT(*) > 5",
  "last": "FOR UPDATE"
}
```

### 高级查询/更新特性
- **原子更新**：`"incrBy": { "score": 10 }` (生成 SQL: `SET score = score + 10`)
  > [!TIP]
  > 在更新场景下，只需在 `UpdateCondition` 中传入 `incrBy` 映射，并调用 `userRepository.updateByDsl(entity, condition)` 即可触发。
- **分组聚合**：`"groupBy": ["department"]` + `"having": "COUNT(*) > 5"` (仅适用于查询)
- **自定义追加**：`"last": "FOR UPDATE"` (将片段追加到 SQL 末尾)
- **自动映射**：前端使用 `camelCase`，后端自动映射为数据库 `snake_case`。

---

## 🔍 DSL 操作符完整列表 (22 个)

这些操作符用于构建 `WHERE` 子句。由于 `WrapperBuilder` 的通用性，这些操作符不仅适用于 **查询 (Query)**，同样适用于 **更新 (Update)** 和 **删除 (Delete)** 场景中的条件过滤。

| 分类     | 操作符                                                                                          | 说明                                         | 生成 SQL 示例                        |
|:-------|:---------------------------------------------------------------------------------------------|:-------------------------------------------|:---------------------------------|
| **比较** | `_eq`, `_ne`, `_gt`, `_gte`, `_lt`, `_lte`                                                   | 等于, 不等于, 大于, 大于等于, 小于, 小于等于                | `age > 18`                       |
| **模糊** | `_like`, `_like_left`, `_like_right`, `_not_like`, `_not_like_left`, `_not_like_right`        | 包含, 以...结尾, 以...开始, 不包含, 不以...结尾, 不以...开始   | `name LIKE '%val%'`              |
| **集合** | `_in`, `_nin`                                                                                 | 在数组中, 不在数组中                                | `id IN (1, 2)`                   |
| **空值** | `_is_null`, `_is_not_null`                                                                    | 是否为 NULL                                   | `deleted_at IS NULL`             |
| **区间** | `_between`, `_not_between`                                                                    | 在范围内 `[start, end]`, 不在范围内                 | `age BETWEEN 10 AND 20`          |
| **逻辑** | `_and`, `_or`, `_not`                                                                         | 逻辑与分组, 逻辑或分组, 逻辑非                          | `(cond1 OR cond2)`               |

---

## 🎯 最佳实践

1. **优先使用 Repository**：封装 DSL 逻辑，保持 Service 简洁。
2. **LIKE 查询不传 `%`**：后端会自动添加（`_like` 对应 `%val%`）。
3. **更新语义分工**：业务新值放在 `entity` 上（非 `null` 才写入并触发审计填充），需置 `NULL` 的列用 `setNull(field)` 或 `setNulls(dto)`，原子增减用 `incrBy`/`decrBy`。
4. **合理使用 `select`**：在大表查询时减少 I/O 压力。
5. **`groupBy` 仅适用于查询**：`groupBy` 和 `having` 仅在 `QueryCondition` 中生效，不适用于更新/删除。
6. **实体类常量定义**：为了支持 type-safe 查询且能访问父类（`BaseEntity`）字段，实体类必须手动定义 `Fields` 内部类：
   ```java
   @FieldNameConstants
   public class User extends BaseEntity {
       // ... 字段定义 ...
       public static final class Fields extends BaseEntity.Fields {}
   }
   ```

---

## 📄 License
Apache License 2.0
