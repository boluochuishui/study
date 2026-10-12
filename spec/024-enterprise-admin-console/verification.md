# 验证记录

## 自动化验证

| 检查项 | 命令 | 结果 |
| --- | --- | --- |
| TypeScript | `npm run type-check` | 通过 |
| ESLint | `npm run lint` | 通过，0 警告 |
| Prettier | `npm run format:check` | 通过 |
| 单元测试 | `npm run test` | 2 个测试文件、5 个用例通过 |
| 生产构建 | `npm run build` | 通过 |
| 生产依赖安全 | `npm audit --registry=https://registry.npmjs.org --omit=dev` | 0 个漏洞 |

## 浏览器验收

- 在 `1440 x 900` 视口验证登录、工作台、规则管理、配置管理和检测日志。
- 在 `390 x 844` 视口验证移动导航、指标卡、筛选区和日志表格。
- 验证规则校验、配置编辑弹窗、检测链路详情和中英文切换。
- 浏览器控制台无 JavaScript 错误。

## Docker 验证

Docker Engine `28.4.0` 可用，已执行镜像构建。当前网络无法连接 `auth.docker.io:443`，因此基础镜像授权和拉取未完成；本地前端生产构建已通过。
