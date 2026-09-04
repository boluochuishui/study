# 验证记录

## 验证命令

```bash
.\mvnw.cmd test
```

## 验证结果

- 构建成功。
- Spring 上下文启动成功。
- TEXT 请求可以路由到 `text.detect.chain` 并返回 `PASS`。
- IMAGE 请求可以路由到 `image.detect.chain`，OCR 衍生文本命中关键词后返回 `REJECT`。
- AUDIO 请求可以路由到 `audio.detect.chain`，ASR 衍生文本命中关键词后返回 `REJECT`。
- VIDEO 请求可以路由到 `video.detect.chain`，抽帧 OCR 衍生文本命中关键词后返回 `REJECT`。
- 链路过程日志可以输出 `CHAIN_START`、`EXECUTE_START`、`EXECUTE_END`、`FINALLY_START`、`FINALLY_END`、`CHAIN_END`。

## 已知缺口

- 当前模型节点均为 mock 实现。
- 视频链仍是同步链，后续应迁移到异步任务。
