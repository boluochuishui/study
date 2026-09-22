# 013 设计

`AuthenticationFilter` 继续先于 Controller 匹配鉴权规则。当前检测 URL 的规则均为 `SIGNATURE`，签名认证会校验 `X-App-Id` 对应的凭证，并将该值纳入签名载荷。Controller 使用 `@RequestHeader("X-App-Id")` 读取同一个请求头，向 `DetectService` 传递调用方标识。

该设计只适用于检测 URL 保持可信的签名鉴权。未来若将检测 URL 改为匿名鉴权或其他不验证 `X-App-Id` 的模式，不得直接将请求头视为可信身份；应同步调整调用方身份来源和鉴权策略。
