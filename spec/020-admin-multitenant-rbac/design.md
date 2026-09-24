# 020 设计：管理台多租户 RBAC

## 身份模型

- `admin_tenant`：租户。
- `admin_user`：租户内用户，用户名在租户内唯一，密码使用 BCrypt。
- `admin_role`：租户内角色。
- `admin_permission`：全局权限目录。
- `admin_user_role`、`admin_role_permission`：用户、角色、权限关联。
- `admin_access_token`：只保存随机令牌的 SHA-256 摘要、有效期和撤销状态。

## 请求链路

登录接口校验租户、用户和密码后签发随机令牌。其余 `/api/admin/**` 请求由过滤器读取
Bearer Token，通过数据库恢复用户当前角色和权限，并写入仅限当前线程的安全上下文。
控制器方法上的权限注解由拦截器执行校验，请求结束后必须清理上下文。

## 租户隔离

数据库 SDK 使用显式 `tenantId` 参数，不使用可遗漏或跨线程泄漏的隐式 ThreadLocal SQL 插件。
所有业务表、唯一索引、Mapper XML 和运行时热加载查询都包含 `tenant_id`。管理台 Service
只能从认证上下文取得租户，客户端不能在请求体中指定租户。

## 权限目录

- `CONFIG_READ`、`CONFIG_WRITE`
- `RULE_READ`、`RULE_WRITE`、`RULE_PUBLISH`
- `IAM_READ`、`IAM_WRITE`

默认租户包含 `SUPER_ADMIN` 角色并授予全部权限。首次启动通过
`ADMIN_BOOTSTRAP_PASSWORD` 创建管理员；数据库迁移和配置文件不包含默认明文密码。

