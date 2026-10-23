# 需求 036：修正子模块 Maven 根目录定位

IDEA 在 `admin-service` 目录单独执行 `compile -f pom.xml` 时，将公共 Checkstyle 配置定位到不存在的 `admin-service/config`，导致构建失败。需要兼容子模块构建和根目录 Reactor 构建，并保持质量检查有效。
