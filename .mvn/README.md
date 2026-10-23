# Maven 根目录标记

保留并提交本目录。从子模块目录执行 Maven 时，Maven 启动脚本会向上查找 `.mvn`，据此设置 `maven.multiModuleProjectDirectory`。

根 POM 的 Checkstyle 和 SpotBugs 使用该属性定位 `config/checkstyle/checkstyle.xml` 和 `config/spotbugs/exclude.xml`。没有此目录时，在 IDEA 中单独运行子模块的 Maven 目标可能会错误地查找 `子模块/config/...`。

本目录仅提供根目录标记，不添加 Maven Wrapper 或 JVM 参数。内部 SDK 依赖仍需通过根目录 `-am` 构建，或预先执行 `install` 安装到本地仓库。
