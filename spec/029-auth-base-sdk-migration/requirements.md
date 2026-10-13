# 需求 029：鉴权模块迁移至 Base SDK

## 背景

`access-service` 内的 `org.example.study.baseSdk.auth` 包属于公共鉴权能力，但物理上仍由接入服务维护，其他微服务无法直接依赖。

## 验收要求

- 鉴权协议、签名、凭证及 nonce 扩展点、规则快照、日志和 HTTP 过滤器归属 `base-sdk`。
- `base-sdk` 不依赖 `access-service` 或 `config-data`。
- 数据库规则读取和检测接口的初始规则归属 `access-service`。
- 接入服务已有同步/异步接口鉴权行为、错误码和响应字段不变。
- SDK 由显式配置启用，其他微服务引入依赖时不自动启用接入服务鉴权。
