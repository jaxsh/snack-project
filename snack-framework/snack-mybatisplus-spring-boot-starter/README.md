# Snack MyBatis-Plus Spring Boot Starter

基于 MyBatis-Plus 的增强型查询模块，提供 **GraphQL 风格的通用查询 API**，让前端可以灵活构建复杂查询条件。

---

## 📦 核心功能

- ✅ **通用查询构建器**：支持 GraphQL 风格的查询语法
- ✅ **19 种查询操作符**：覆盖所有常见查询场景
- ✅ **无限嵌套逻辑**：支持 `_and`、`_or`、`_not` 组合
- ✅ **字段选择与排序**：类似 GraphQL 的 `select` 和 `orderBy`
- ✅ **安全性**：自动字段验证，防止 SQL 注入

---

## 🚀 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'org.jax.snack.framework:snack-mybatisplus-spring-boot-starter'
}
```

### 2. Controller 示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;
    private final QueryWrapperBuilder builder;

    @PostMapping("/query")
    public List<User> queryUsers(@RequestBody QueryCondition condition) {
        QueryWrapper<User> wrapper = builder.build(condition, User.class);
        return userMapper.selectList(wrapper);
    }
}
```

---

## 📡 前端使用指南

### 基础查询结构

前端发送的 JSON 格式：

```json
{
  "select": ["id", "username", "email"],
  "where": {

  },
  "orderBy": [
    { "field": "createTime", "direction": "desc" }
  ]
}
```

> **说明**：`where` 对象中放置查询条件，详见下文操作符列表。

---

## 🔍 查询操作符完整列表

### 比较操作符（6 个）

| 操作符    | 含义   | 前端示例                                 | 生成 SQL                |
|--------|------|--------------------------------------|-----------------------|
| `_eq`  | 等于   | `{ "age": { "_eq": 25 } }`           | `age = 25`            |
| `_ne`  | 不等于  | `{ "status": { "_ne": "deleted" } }` | `status <> 'deleted'` |
| `_gt`  | 大于   | `{ "age": { "_gt": 18 } }`           | `age > 18`            |
| `_gte` | 大于等于 | `{ "age": { "_gte": 18 } }`          | `age >= 18`           |
| `_lt`  | 小于   | `{ "price": { "_lt": 100 } }`        | `price < 100`         |
| `_lte` | 小于等于 | `{ "price": { "_lte": 100 } }`       | `price <= 100`        |

---

### 模糊匹配（5 个）

| 操作符           | 含义     | 前端示例                                  | 生成 SQL                   | 备注           |
|---------------|--------|---------------------------------------|--------------------------|--------------|
| `_like`       | 模糊匹配   | `{ "name": { "_like": "张" } }`        | `name LIKE '%张%'`        | **前端不需要传 %** |
| `_like_left`  | 后缀匹配   | `{ "name": { "_like_left": "三" } }`   | `name LIKE '%三'`         | 以"三"结尾       |
| `_like_right` | 前缀匹配   | `{ "name": { "_like_right": "张" } }`  | `name LIKE '张%'`         | 以"张"开头       |
| `_ilike`      | 不区分大小写 | `{ "email": { "_ilike": "admin" } }`  | `email LIKE '%admin%'`   | MySQL 默认不区分  |
| `_not_like`   | 否定模糊   | `{ "name": { "_not_like": "test" } }` | `name NOT LIKE '%test%'` | 排除包含 test    |

⚠️ **重要**：前端**不需要**传递 `%` 通配符，后端会自动添加！

---

### 集合操作（2 个）

| 操作符    | 含义    | 前端示例                                             | 生成 SQL                            |
|--------|-------|--------------------------------------------------|-----------------------------------|
| `_in`  | 在范围内  | `{ "status": { "_in": ["active", "pending"] } }` | `status IN ('active', 'pending')` |
| `_nin` | 不在范围内 | `{ "age": { "_nin": [18, 19] } }`                | `age NOT IN (18, 19)`             |

---

### 空值判断（2 个）

| 操作符            | 含义  | 前端示例                                    | 生成 SQL               |
|----------------|-----|-----------------------------------------|----------------------|
| `_is_null`     | 为空  | `{ "deletedAt": { "_is_null": true } }` | `deleted_at IS NULL` |
| `_is_not_null` | 不为空 | `{ "email": { "_is_not_null": true } }` | `email IS NOT NULL`  |

---

### 区间查询（1 个）

| 操作符        | 含义 | 前端示例                                  | 生成 SQL                  |
|------------|----|---------------------------------------|-------------------------|
| `_between` | 区间 | `{ "age": { "_between": [18, 30] } }` | `age BETWEEN 18 AND 30` |

---

### 逻辑操作符（3 个）

| 操作符    | 含义 | 前端示例                     |
|--------|----|--------------------------|
| `_and` | 且  | `{ "_and": [条件1, 条件2] }` |
| `_or`  | 或  | `{ "_or": [条件1, 条件2] }`  |
| `_not` | 非  | `{ "_not": { 条件 } }`     |

---

## 📝 前端查询示例

### 示例 1：简单查询

**需求**：查询年龄大于 18 岁的用户

```json
{
  "where": {
    "age": { "_gt": 18 }
  }
}
```

**生成 SQL**：
```sql
SELECT * FROM user WHERE age > 18
```

---

### 示例 2：多条件 AND

**需求**：查询年龄 18-30 岁且状态为 active 的用户

```json
{
  "where": {
    "_and": [
      { "age": { "_gte": 18 } },
      { "age": { "_lte": 30 } },
      { "status": { "_eq": "active" } }
    ]
  }
}
```

**生成 SQL**：
```sql
SELECT * FROM user
WHERE age >= 18 AND age <= 30 AND status = 'active'
```

---

### 示例 3：OR 查询

**需求**：VIP 用户或消费超过 1000 的用户

```json
{
  "where": {
    "_or": [
      { "isVip": { "_eq": true } },
      { "totalAmount": { "_gt": 1000 } }
    ]
  }
}
```

**生成 SQL**：
```sql
SELECT * FROM user
WHERE is_vip = 1 OR total_amount > 1000
```

---

### 示例 4：模糊搜索

**需求**：搜索用户名包含"张"或邮箱以"@gmail.com"结尾的用户

```json
{
  "where": {
    "_or": [
      { "username": { "_like": "张" } },
      { "email": { "_like_left": "@gmail.com" } }
    ]
  }
}
```

⚠️ **注意**：前端传 `"张"` 即可，**不需要传 `"%张%"`**！

**生成 SQL**：
```sql
SELECT * FROM user
WHERE username LIKE '%张%' OR email LIKE '%@gmail.com'
```

---

### 示例 5：复杂嵌套查询

**需求**：
- 用户名包含"admin"
- 且（年龄在 18-30 之间 OR 年龄大于等于 65）
- 且用户名不是 "system"

```json
{
  "select": ["id", "username", "age"],
  "where": {
    "_and": [
      { "username": { "_like": "admin" } },
      {
        "_or": [
          { "age": { "_between": [18, 30] } },
          { "age": { "_gte": 65 } }
        ]
      },
      { "username": { "_ne": "system" } }
    ]
  },
  "orderBy": [
    { "field": "age", "direction": "desc" }
  ]
}
```

**生成 SQL**：
```sql
SELECT id, username, age
FROM user
WHERE (
  username LIKE '%admin%'
  AND (age BETWEEN 18 AND 30 OR age >= 65)
  AND username <> 'system'
)
ORDER BY age DESC
```

---

### 示例 6：NOT 查询

**需求**：非 VIP 且未被删除的用户

```json
{
  "where": {
    "_and": [
      { "_not": { "isVip": { "_eq": true } } },
      { "deletedAt": { "_is_null": true } }
    ]
  }
}
```

**生成 SQL**：
```sql
SELECT * FROM user
WHERE NOT (is_vip = 1) AND deleted_at IS NULL
```

---

## 🎨 字段选择与排序

### 字段选择（select）

```json
{
  "select": ["id", "username", "email"]
}
```

生成：`SELECT id, username, email FROM user`

**省略 select**：默认查询所有字段（`SELECT *`）

---

### 排序（orderBy）

```json
{
  "orderBy": [
    { "field": "createTime", "direction": "desc" },
    { "field": "id", "direction": "asc" }
  ]
}
```

生成：`ORDER BY create_time DESC, id ASC`

**direction 取值**：
- `"asc"` 或 `"ASC"` → 升序
- `"desc"` 或 `"DESC"` → 降序

---

## ⚠️ 前端常见问题

### 1. **LIKE 查询需要传 % 吗？**

❌ **不需要！** 后端会自动添加：

**✅ 正确写法**：
```json
{ "name": { "_like": "张三" } }
```

**❌ 错误写法**（会被当作普通字符）：
```json
{ "name": { "_like": "%张三%" } }
```

---

### 2. **如何实现"以...开头"？**

使用 `_like_right`：

```json
{ "username": { "_like_right": "admin" } }
```

生成：`username LIKE 'admin%'`

---

### 3. **如何查询多个值？**

使用 `_in`：

```json
{ "status": { "_in": ["active", "pending", "processing"] } }
```

---

### 4. **如何排除某些值？**

使用 `_nin`（NOT IN）：

```json
{ "role": { "_nin": ["guest", "banned"] } }
```

---

### 5. **字段名如何映射？**

前端使用**驼峰命名**，后端自动转**下划线命名**：

| 前端字段         | 数据库字段         |
|--------------|---------------|
| `createTime` | `create_time` |
| `userId`     | `user_id`     |
| `isVip`      | `is_vip`      |

通过 MyBatis-Plus 的 `@TableField` 注解映射。

---

## 🔒 安全性

### 字段验证

后端会**自动验证**字段是否存在：

```json
{
  "where": {
    "nonExistentField": { "_eq": "value" }
  }
}
```

> ❌ **结果**：`nonExistentField` 会被忽略

**日志输出**：
```
Field nonExistentField does not exist in entity User, ignored
```

### SQL 注入防护

所有参数使用 **MyBatis 预编译**，自动防止 SQL 注入：

```json
{ "username": { "_eq": "'; DROP TABLE user; --" } }
```

生成：
```sql
SELECT * FROM user WHERE username = ?
-- 参数: '; DROP TABLE user; --（作为普通字符串处理）
```

---

## 🛠️ 后端集成示例

### 完整 Controller 示例

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final QueryWrapperBuilder queryBuilder;

    /**
     * 通用查询接口
     */
    @PostMapping("/query")
    public ResponseEntity<List<User>> queryUsers(
            @RequestBody QueryCondition condition) {

        QueryWrapper<User> wrapper = queryBuilder.build(condition, User.class);
        List<User> users = userMapper.selectList(wrapper);

        return ResponseEntity.ok(users);
    }

    /**
     * 分页查询接口
     */
    @PostMapping("/query/page")
    public ResponseEntity<Page<User>> queryUsersPage(
            @RequestBody QueryCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        QueryWrapper<User> wrapper = queryBuilder.build(condition, User.class);
        Page<User> pageResult = userMapper.selectPage(
            new Page<>(page, size), wrapper
        );

        return ResponseEntity.ok(pageResult);
    }
}
```

---

## 📚 API 参考

### QueryCondition 对象

```typescript
interface QueryCondition {
  select?: string[];
  where?: Record<string, any>;
  orderBy?: OrderByCondition[];
}

interface OrderByCondition {
  field: string;
  direction: 'asc' | 'desc';
}
```

**字段说明**：
- `select`: 字段选择（可选）
- `where`: 查询条件（可选）
- `orderBy`: 排序条件（可选）
- `field`: 字段名
- `direction`: 排序方向（`'asc'` 或 `'desc'`）

### 完整操作符列表

| 分类     | 操作符                                                         |
|--------|-------------------------------------------------------------|
| **比较** | `_eq`, `_ne`, `_gt`, `_gte`, `_lt`, `_lte`                  |
| **模糊** | `_like`, `_like_left`, `_like_right`, `_ilike`, `_not_like` |
| **集合** | `_in`, `_nin`                                               |
| **空值** | `_is_null`, `_is_not_null`                                  |
| **区间** | `_between`                                                  |
| **逻辑** | `_and`, `_or`, `_not`                                       |

---

## 🎯 最佳实践

1. ✅ **使用 `_and` 明确表达复杂条件**
2. ✅ **LIKE 查询不传 `%`，让后端处理**
3. ✅ **使用 `select` 减少数据传输**
4. ✅ **多表查询使用 JOIN，不要用嵌套查询**
5. ⚠️ **避免在大表上使用 `_like` 全表扫描**

---

## 📄 License

Apache License 2.0
