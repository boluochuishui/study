# 设计

- 父 POM dependencyManagement 管理 org.apache.commons:commons-lang3:3.20.0 和 org.apache.commons:commons-collections4:4.6.0。
- base-sdk 显式依赖两者，不设置 optional；依赖 base-sdk 的现有模块可传递获得工具。未来独立 SDK 如直接使用 Commons，应显式声明自己的依赖并沿用父 POM 版本。
- 字符串判空使用 org.apache.commons.lang3.StringUtils；大小写比较使用 org.apache.commons.lang3.Strings.CI/CS。
- 集合使用 org.apache.commons.collections4.CollectionUtils，Map 使用 MapUtils；不能将 Map 传给 Apache CollectionUtils.isEmpty。
- Commons isBlank 使用 Character.isWhitespace 判断空白，包含制表符和 Unicode 空白；不要用它替代仅判断长度为零的 isEmpty。
- 仅替换责任链配置校验与 MDC Map 恢复中的混用导包。签名与令牌校验不修改。
