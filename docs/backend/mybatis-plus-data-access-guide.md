# MyBatis-Plus 数据访问：Java 对象怎样读写数据库（小白版）

MyBatis-Plus 可以理解为“实体类、Mapper 和 SQL 之间的翻译器”。简单的按主键查询、插入、更新、分页不必每次手写 SQL；复杂查询仍可以放进 Mapper 注解或 XML。业务规则仍应放在 Service，不能因为 Mapper 很方便就把权限判断塞进数据库层。

## 完整流程

```text
Controller 收到请求
  -> Service 校验参数、身份、权限和事务
  -> Mapper 调用 MyBatis-Plus（insert/selectById/selectPage/updateById）
  -> Entity 字段映射到表字段
  -> MyBatis-Plus 根据配置生成/执行 SQL
  -> Service 组装 DTO / PageResponse
  -> Controller 返回
```

## 关键文件地图

| 作用 | 真实代码位置 |
| --- | --- |
| MyBatis-Plus 配置 | `backend/land/src/main/java/com/skylink/land/config/MybatisPlusConfiguration.java:1-79` |
| YAML 全局配置 | `backend/land/src/main/resources/application.yaml` 的 `mybatis-plus` 段 |
| 表结构 | `backend/land/src/main/resources/schema.sql` |
| 任务实体示例 | `backend/land/src/main/java/com/skylink/land/entity/task/Task.java` |
| 任务 Mapper 示例 | `backend/land/src/main/java/com/skylink/land/mapper/task/TaskMapper.java` |
| 使用 Mapper 的 Service 示例 | `backend/land/src/main/java/com/skylink/land/service/task/impl/TaskServiceImpl.java:61-182` |

## 分页插件为什么要注册

代码位置：`MybatisPlusConfiguration.java:68-78`

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor(); // 1. 创建 MyBatis-Plus 插件总入口。
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
    // 2. 告诉插件数据库是 MySQL，并启用分页 SQL 改写能力。
    return interceptor; // 3. Spring 把它交给 MyBatis 使用。
}

@Bean
public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory); // 4. 提供线程安全的 SQL 会话模板。
}
```

没有分页插件时，`selectPage` 可能不能按数据库方言正确追加分页条件，或无法得到总数。

## YAML 中的几个关键开关

代码位置：`backend/land/src/main/resources/application.yaml` 的 `mybatis-plus` 配置段。

```yaml
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml # 1. Mapper XML 若存在，从这里扫描。
  type-aliases-package: com.skylink.land.entity # 2. 实体包可在 XML 中使用简短类型名。
  configuration:
    map-underscore-to-camel-case: true # 3. create_time 能自动映射为 createTime。
  global-config:
    db-config:
      id-type: assign_id # 4. 主键策略由框架统一处理。
      logic-delete-field: deleted # 5. deleted 是逻辑删除标记字段。
      logic-delete-value: 1 # 6. 1 表示已删除。
      logic-not-delete-value: 0 # 7. 0 表示正常数据。
```

“下划线转驼峰”减少了实体字段和数据库列名之间的重复标注；但字段名差异很大时，实体上仍要明确使用映射注解。

## Service 怎样用 Mapper

代码位置：`TaskServiceImpl.java:68-78`、`110-112`、`150-151`

```java
taskMapper.insert(task); // 1. 简单新增：实体 -> INSERT。

Page<Task> page = taskMapper.selectPage(
    query.toMybatisPage(), // 2. 请求分页参数转为 MyBatis-Plus Page 对象。
    wrapper // 3. LambdaQueryWrapper 组合 where、排序等条件。
);

taskMapper.updateById(task); // 4. 用 task 的主键定位记录并更新。
```

`LambdaQueryWrapper<Task>` 的好处是写 `Task::getStatus` 而不是硬编码字符串列名。重构 Java 字段时，IDE 更容易发现问题。

## 逻辑删除不等于“从硬盘消失”

当前全局配置把 `deleted` 设为逻辑删除字段。通常 `deleteById` 会把该字段标记为已删除，普通查询自动过滤；数据仍在表里，便于审计或恢复。具体是否走逻辑删除还取决于实体上的 `@TableLogic` 等映射配置，因此排查时要同时查看实体与生成 SQL。

任务删除调用在 `TaskServiceImpl.java:176-182`，但删除前先验证创建者或管理员身份——数据库访问层并不知道“谁有权删除”，这是 Service 的职责。

## 事务应该放在哪里

代码位置：`TaskServiceImpl.java:61-63`、`122-124`、`154-156`、`176-178`

```java
@Transactional(rollbackFor = Exception.class) // 1. 标记整个业务方法为一个数据库事务。
public void deleteTask(Long currentUserId, Long taskId) {
    Task task = requireTask(taskId); // 2. 先读并检查目标是否存在。
    requireCreatorOrAdministrator(currentUserId, task); // 3. 先完成越权检查。
    taskMapper.deleteById(taskId); // 4. 只有检查通过才执行删除。
}
```

事务边界通常以“一个完整业务动作”为单位，而不是“每一条 SQL”一个事务。多张表必须一起改变时尤其重要，例如建群时插入群和群成员。

## 常见误解

| 误解 | 实际情况 |
| --- | --- |
| 有 MyBatis-Plus 就完全没有 SQL | 简单 CRUD 少写 SQL；复杂查询仍可用注解/XML。 |
| Mapper 应负责权限判断 | 不应；Mapper 负责数据访问，Service 负责业务与授权。 |
| `deleteById` 一定物理删除 | 当前项目配置了逻辑删除，需看实体映射和最终 SQL。 |
| 分页只改前端 page 参数就够 | 后端还需 Page 对象和分页拦截器。 |

## 人话复盘

实体描述“数据长什么样”，Mapper 负责“怎么和表说话”，Service 决定“这次能不能做、要一起做哪些事”。MyBatis-Plus 帮你省掉重复 CRUD，但不替你设计权限和业务规则。
