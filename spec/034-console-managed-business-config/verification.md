# 验证记录

## SpotBugs 修正回归

- `SaveManagedResourceRequest` 在 setter/getter 两端拷贝顶层 Map，保留允许清空字段的 null 值；不使用拒绝 null 的 `Map.copyOf`。
- 数字字段只进行一次 Number 类型判断，再执行有限数、整数和范围检查，移除重复 `instanceof`。
- `mvn -pl admin-service -am test '-Dtest=SaveManagedResourceRequestTests,AdminManagedResourceServiceTests' '-Dsurefire.failIfNoSpecifiedTests=false'`：13 个测试通过，Checkstyle 通过。
- `mvn -f admin-service/pom.xml spotbugs:check`：BugInstance=0、Error=0，BUILD SUCCESS。未增加 SpotBugs 排除规则。

## 原功能验证

- `mvn -pl admin-service -am test`：后端现有测试与新表单单元测试。
- `mvn -pl admin-service -am test '-Dtest=ManagedResourceIntegrationTests' '-Dsurefire.failIfNoSpecifiedTests=false' '-Dmanaged.integration.enabled=true'`：使用本机 MySQL 执行六表创建、编辑、审计、跨租户及版本冲突测试；事务回滚。
- `mvn -pl admin-service -am checkstyle:check`：代码风格。
- `npm run type-check`、`npm run lint`、`npm run test`、`npm run build`：前端类型、规范、测试与构建。
- 本需求触及的前端文件通过 `prettier --check`。仓库级 `npm run format:check` 仍因未改动的 `OnlineTestView.vue` 报格式问题。

数据库集成测试需要本机 `content_risk` 库已执行至 V9，且根目录 `application-local.yaml` 可连接数据库。浏览器中已验证 Mock 环境 App 新增和六资源页面导航；真实登录后的 HTTP 全链路与检测运行期配置加载未在本需求覆盖。
