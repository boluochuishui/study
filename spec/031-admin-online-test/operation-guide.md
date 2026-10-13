# 操作步骤

## 本机数据库连接

本机管理台连接需要设置 `MYSQL_PASSWORD`。项目默认值为空，未设置会收到 MySQL `Access denied ... (using password: NO)`。

本次已在项目根目录补充被 Git 忽略的 `application-local.yaml`。IDEA 启动 `AdminApplication` 时将工作目录设置为 Maven 根目录 `study/study`，激活配置 `local`（Active profiles 或参数 `--spring.profiles.active=local`），即可加载这个外部文件。该文件位于源码资源目录之外，不会被打入 Jar。其他环境继续通过环境变量提供密码。

如果 `admin_user` 为空，首次启动还需设置环境变量 `ADMIN_BOOTSTRAP_PASSWORD` 为自行选择的至少 12 字符密码，服务据此创建默认租户的 `admin` 用户。此密码为管理台登录密码，与 MySQL 密码独立；已有管理台用户时无需再次初始化。

## 在线测试配置

1. 在 MySQL `content_risk` 执行 `config-data/src/main/resources/db/migration/V7__admin_online_test.sql`；已有数据库按 V1 到 V7 顺序执行未应用迁移。该项目使用版本化 SQL 文件，不自动迁移。
2. 管理台配置 `ONLINE_TEST_ACCESS_URL` 指向检测接入服务的**服务端地址**，本地默认 `http://localhost:8080`。
3. 本地默认租户使用 `ONLINE_TEST_APP_ID` 与 `ONLINE_TEST_APP_SECRET`，必须与接入服务对应的鉴权凭证一致。生产环境不要使用默认密钥。其他租户在管理台外部配置文件中设置 `admin.online-test.credentials.<tenantCode>.app-id` 和 `.secret`，切勿写入管理台数据库或前端环境变量。
4. 使用有 `CONFIG_READ`、`CONFIG_WRITE`、`TEST_RUN`、`TEST_READ` 权限的账号进入在线测试页，按类型填写 App ID 和场景编码并保存。配置值格式为 JSON：`{"appId":"demo-app","sceneCode":"text.default"}`，配置 SDK 命名空间为 `console.online-test`，键为 `TEXT`/`IMAGE`/`AUDIO`/`VIDEO`。
5. 输入文本、URL 或选择本地文件后执行测试；单图片不超过 10 MiB，单音视频不超过 40 MiB，本地文件总量不超过 40 MiB（为 Base64 和 JSON 预留请求体空间）。文本/图片同步仅一项；异步文本/图片最多 100 项，异步文本勾选“每行一个样本”后才按行拆分；音视频一项。进入“测试记录”可按时间查看最近 100 条。
6. 异步响应 `ACCEPTED` 仅表示接入服务受理；最终判定仍以结果 MQ 为准。若接入服务不可达，页面显示调用失败，记录状态为 `FAILED`。

生产部署应通过密钥管理系统或 Kubernetes Secret 注入服务端凭证，并限制管理台到接入服务的网络访问。当前接入 URL 为单一固定服务地址，不支持在浏览器中指定上游地址。
