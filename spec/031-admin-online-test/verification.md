# 验证

- `mvn -pl admin-service -am verify`：通过，包含单元测试、Checkstyle 与 SpotBugs。
- 新增服务测试使用本地 HTTP 测试端点验证 HMAC 签名、租户凭证隔离，以及记录不含样本原文。
- `npm run build`、`npm run lint`、`npm run test`：通过。
- 本地 `VITE_ENABLE_MOCK=true` 页面检查：文本同步返回、记录列表、音频仅异步和 `ACCEPTED` 提示均正常。
- 2026-10-13 本地 MySQL 验证：`localhost:3306/content_risk` 可连接；已执行 V7 并确认测试记录表和 `TEST_READ/TEST_RUN` 权限存在。
- 管理台 Jar 使用 `local` 配置连接真实 MySQL，Hikari 日志显示 `Added connection`、`Start completed`。当前没有管理台用户，启动随后因缺少至少 12 字符的 `ADMIN_BOOTSTRAP_PASSWORD` 停止，数据库连接已正常。
- 尚未对真实检测接入服务做端到端联调；初始化管理员并启动对应服务后，按 `operation-guide.md` 验证。
