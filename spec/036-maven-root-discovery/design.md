# 设计

现有 Checkstyle 和 SpotBugs 都通过 `${maven.multiModuleProjectDirectory}/config/...` 定位公共规则。根目录缺少 `.mvn` 时，从子模块调用 Maven 启动脚本会以当前子模块作为根目录。

新增并提交 `.mvn/README.md`，使 `.mvn` 目录被 Git 保留。Maven 启动脚本向上发现该目录后，将公共根目录传入构建，继续复用现有插件配置。此修改不跳过 Checkstyle，不复制规则，也不改变内部 SDK 的依赖安装要求。
