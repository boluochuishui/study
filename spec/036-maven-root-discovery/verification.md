# 验证记录

修复前使用 IDEA 内置 `mvn.cmd`，从 `admin-service` 执行 `compile -f pom.xml`，复现 `admin-service/config/checkstyle/checkstyle.xml` 缺失错误。

修复后：

- 使用相同 IDEA 内置 Maven、相同 settings.xml 和本地仓库，从 `admin-service` 执行 `compile -f pom.xml`，Checkstyle 零违规，编译 45 个源文件，BUILD SUCCESS。
- 从项目根目录执行 `mvn -pl admin-service -am validate`，父工程、两个 SDK 和管理服务全部成功。
- Checkstyle 和 SpotBugs 均使用未修改的 `${maven.multiModuleProjectDirectory}` 定位规则；本次未重新执行完整 SpotBugs 扫描或单元测试。
