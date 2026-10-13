# 需求 031：管理台在线测试

## 范围

- 管理台提供文本、图片、音频、视频测试入口与测试记录视图。
- 在线测试必须通过管理台后端代签，进入现有 `access-service` 的同步/异步 HTTP 接口。
- 文本、图片允许同步或异步；音频、视频只允许异步。异步受理不等于最终检测通过。
- 每个租户、每种类型在 `console.online-test` 命名空间配置 `appId`、`sceneCode`；服务端凭证按租户从外部配置加载。
- 记录存管理台 MySQL，按 `tenant_id` 隔离，只保存请求元数据和返回结果，不保存原始文本、URL 或 Base64。
- 管理台沿用 RBAC：`TEST_RUN` 执行，`TEST_READ` 查询记录，`CONFIG_READ/WRITE` 修改测试配置。
