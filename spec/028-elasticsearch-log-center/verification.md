# 验证

- `AdminLogApiTests`：租户条件、字段白名单、无权限拒绝、时间区间限制。
- `mvn -pl admin-service -am verify`：通过，Checkstyle/SpotBugs/JaCoCo 通过；本次模块链路包含 21 个管理台测试，新增 2 个日志 API 测试。
- 根目录 `mvn verify`：全部模块通过。
- 前端 `npm run build`、`npm run lint`、`npm run test`：通过；前端现有 5 个单元测试通过。
- 浏览器 Mock 模式人工检查日志列表、任务抽屉和窄屏布局，通过。
- Docker Desktop 引擎当前不可用，未进行真实 ES 与 Agent 联调；运维步骤需在目标环境验证。
