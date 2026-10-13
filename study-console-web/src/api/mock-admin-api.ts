import type { AdminApi } from './contracts'
import type {
  AdminIdentity,
  AdminRole,
  AdminSession,
  AdminUser,
  AuditLog,
  LogEvent,
  ConfigItem,
  DashboardSnapshot,
  PermissionCode,
  PermissionItem,
  RuleDraft,
  RuleRelease,
  RuleSet,
  DetectContentType,
  OnlineTestProfile,
  OnlineTestRecord,
} from '@/types/admin'

const permissions: PermissionCode[] = [
  'CONFIG_READ',
  'CONFIG_WRITE',
  'RULE_READ',
  'RULE_WRITE',
  'RULE_PUBLISH',
  'IAM_READ',
  'IAM_WRITE',
  'LOG_READ',
  'TEST_READ',
  'TEST_RUN',
]

const testProfiles: Partial<Record<DetectContentType, OnlineTestProfile>> = {
  TEXT: { appId: 'demo-app', sceneCode: 'text.default' },
  IMAGE: { appId: 'demo-app', sceneCode: 'image.default' },
  AUDIO: { appId: 'demo-app', sceneCode: 'audio.default' },
  VIDEO: { appId: 'demo-app', sceneCode: 'video.default' },
}
const testRecords: OnlineTestRecord[] = []

const identity: AdminIdentity = {
  tenantId: 1,
  tenantCode: 'default',
  userId: 1,
  username: 'admin',
  displayName: '平台管理员',
  permissions,
}

const ruleSets: RuleSet[] = [
  {
    id: 1,
    tenantId: 1,
    ruleSetCode: 'text.default',
    ruleSetName: '文本默认规则集',
    scene: 'TEXT',
    evaluationMode: 'FIRST_MATCH',
    errorPolicy: 'CONTINUE',
    publishedVersion: 12,
    version: 18,
    enabled: true,
    createdAt: '2026-09-12T09:10:00',
    updatedAt: '2026-09-30T09:32:00',
  },
  {
    id: 2,
    tenantId: 1,
    ruleSetCode: 'image.default',
    ruleSetName: '图片默认规则集',
    scene: 'IMAGE',
    evaluationMode: 'ALL_MATCH',
    errorPolicy: 'CONTINUE',
    publishedVersion: 7,
    version: 9,
    enabled: true,
    createdAt: '2026-09-14T10:00:00',
    updatedAt: '2026-09-29T16:21:00',
  },
  {
    id: 3,
    tenantId: 1,
    ruleSetCode: 'video.live',
    ruleSetName: '直播视频规则集',
    scene: 'VIDEO',
    evaluationMode: 'ALL_MATCH',
    errorPolicy: 'REVIEW',
    publishedVersion: 3,
    version: 6,
    enabled: false,
    createdAt: '2026-09-18T15:10:00',
    updatedAt: '2026-09-27T12:11:00',
  },
]

const rulesBySet: Record<string, RuleDraft[]> = {
  'text.default': [
    {
      id: 11,
      tenantId: 1,
      ruleSetId: 1,
      ruleId: 'text.keyword.reject',
      ruleName: '高风险关键词拒绝',
      expression: "#labels.contains('keyword_risk')",
      priority: 10,
      attributesJson: '{"action":"REJECT"}',
      version: 5,
      enabled: true,
      createdAt: '2026-09-12T09:20:00',
      updatedAt: '2026-09-30T09:31:00',
    },
    {
      id: 12,
      tenantId: 1,
      ruleSetId: 1,
      ruleId: 'text.contact.review',
      ruleName: '联系方式人工复审',
      expression: "#labels.contains('contact_risk') && #score >= 0.6",
      priority: 20,
      attributesJson: '{"action":"REVIEW"}',
      version: 3,
      enabled: true,
      createdAt: '2026-09-12T09:22:00',
      updatedAt: '2026-09-28T11:08:00',
    },
  ],
  'image.default': [
    {
      id: 21,
      tenantId: 1,
      ruleSetId: 2,
      ruleId: 'image.explicit.review',
      ruleName: '图片敏感内容复审',
      expression: "#scores['explicit'] >= 0.72",
      priority: 10,
      attributesJson: '{"action":"REVIEW"}',
      version: 2,
      enabled: true,
      createdAt: '2026-09-14T10:30:00',
      updatedAt: '2026-09-29T16:20:00',
    },
  ],
  'video.live': [],
}

const releases: RuleRelease[] = [
  {
    releaseId: 120,
    tenantId: 1,
    ruleSetCode: 'text.default',
    releaseVersion: 12,
    scene: 'TEXT',
    snapshotJson: '{}',
    checksum: 'a93e4b27f4d2',
    deleted: false,
    publishedBy: 'zhang.san',
    publishedAt: '2026-09-30T09:35:00',
  },
  {
    releaseId: 72,
    tenantId: 1,
    ruleSetCode: 'image.default',
    releaseVersion: 7,
    scene: 'IMAGE',
    snapshotJson: '{}',
    checksum: 'd1097adcc850',
    deleted: false,
    publishedBy: 'li.si',
    publishedAt: '2026-09-29T16:24:00',
  },
]

let configs: ConfigItem[] = [
  {
    id: 1,
    tenantId: 1,
    namespace: 'detect.text',
    configKey: 'cache.violation.ttl',
    configValue: '7d',
    version: 4,
    enabled: true,
    description: '违规文本缓存有效期',
    createdAt: '2026-09-16T10:00:00',
    updatedAt: '2026-09-30T08:45:00',
  },
  {
    id: 2,
    tenantId: 1,
    namespace: 'detect.text',
    configKey: 'large-model.enabled',
    configValue: 'false',
    version: 2,
    enabled: true,
    description: '异步文本大模型检测开关',
    createdAt: '2026-09-20T14:00:00',
    updatedAt: '2026-09-27T17:22:00',
  },
  {
    id: 3,
    tenantId: 1,
    namespace: 'auth.routing',
    configKey: 'signature.paths',
    configValue: '/api/detect/**',
    version: 6,
    enabled: true,
    description: 'HMAC 签名鉴权路径',
    createdAt: '2026-09-10T09:00:00',
    updatedAt: '2026-09-30T08:02:00',
  },
]

const roles: AdminRole[] = [
  {
    id: 1,
    tenantId: 1,
    roleCode: 'SUPER_ADMIN',
    roleName: '超级管理员',
    enabled: true,
    permissions,
  },
  {
    id: 2,
    tenantId: 1,
    roleCode: 'RULE_OPERATOR',
    roleName: '规则运营',
    enabled: true,
    permissions: ['RULE_READ', 'RULE_WRITE', 'RULE_PUBLISH', 'CONFIG_READ'],
  },
  {
    id: 3,
    tenantId: 1,
    roleCode: 'AUDITOR',
    roleName: '审计员',
    enabled: true,
    permissions: ['RULE_READ', 'CONFIG_READ', 'IAM_READ'],
  },
]

const users: AdminUser[] = [
  {
    id: 1,
    tenantId: 1,
    username: 'admin',
    displayName: '平台管理员',
    enabled: true,
    roleCodes: ['SUPER_ADMIN'],
  },
  {
    id: 2,
    tenantId: 1,
    username: 'zhang.san',
    displayName: '张三',
    enabled: true,
    roleCodes: ['RULE_OPERATOR'],
  },
  {
    id: 3,
    tenantId: 1,
    username: 'li.si',
    displayName: '李四',
    enabled: false,
    roleCodes: ['AUDITOR'],
  },
]

const auditLogs: AuditLog[] = [
  {
    id: '1',
    occurredAt: '2026-09-30T10:42:18',
    taskId: 'dt_93bafff817604702b22f4dc4caf62bc5',
    traceId: 'trace_f486ce36fe1d4d6b8357a773dcc051e6',
    appId: 'demo-app',
    sceneCode: 'comment',
    contentType: 'TEXT',
    action: 'REJECT',
    durationMs: 21,
    degraded: false,
    labels: ['keyword_risk'],
    nodes: [
      { name: 'HTTP 接入与鉴权', status: 'success', durationMs: 3, message: 'HMAC verified' },
      { name: '文本标准化', status: 'success', durationMs: 0, message: 'normalized' },
      { name: '违规缓存查询', status: 'warning', durationMs: 18, message: 'cache hit' },
      { name: '决策输出', status: 'success', durationMs: 0, message: 'REJECT' },
    ],
  },
  {
    id: '2',
    occurredAt: '2026-09-30T10:41:50',
    taskId: 'dt_ed9de1dfc441494db8f6f5946cc144aa',
    traceId: 'trace_3493b769f3d64c99840f8b489245530d',
    appId: 'demo-app',
    sceneCode: 'comment',
    contentType: 'TEXT',
    action: 'PASS',
    durationMs: 48,
    degraded: false,
    labels: [],
    nodes: [
      { name: 'HTTP 接入与鉴权', status: 'success', durationMs: 4, message: 'HMAC verified' },
      { name: '文本检测责任链', status: 'success', durationMs: 41, message: 'no risk found' },
      { name: '响应输出', status: 'success', durationMs: 3, message: 'PASS' },
    ],
  },
  {
    id: '3',
    occurredAt: '2026-09-30T10:38:02',
    taskId: 'dt_7bb1918ca08c4a5d95061fc01de27d98',
    traceId: 'trace_4c06ce4bf51347789f6fb5426b28f080',
    appId: 'media-client',
    sceneCode: 'profile-avatar',
    contentType: 'IMAGE',
    action: 'REVIEW',
    durationMs: 326,
    degraded: false,
    labels: ['explicit_suspected'],
    nodes: [
      { name: 'HTTP 接入与鉴权', status: 'success', durationMs: 5, message: 'HMAC verified' },
      { name: '图片特征检测', status: 'warning', durationMs: 316, message: 'score 0.74' },
      { name: '决策输出', status: 'success', durationMs: 5, message: 'REVIEW' },
    ],
  },
]

const dashboard: DashboardSnapshot = {
  metrics: [
    { key: 'requests', value: '128,406', change: '+12.4%', trend: 'up' },
    { key: 'rejectRate', value: '1.82%', change: '-0.16%', trend: 'down' },
    { key: 'p95', value: '84 ms', change: '-9 ms', trend: 'down' },
    { key: 'degraded', value: '0.03%', change: 'steady', trend: 'steady' },
  ],
  services: [
    {
      name: 'access-service',
      kind: 'HTTP',
      status: 'healthy',
      replicas: '2/2',
      latency: 12,
      updatedAt: '10:42:20',
    },
    {
      name: 'text-detect-service',
      kind: 'Dubbo',
      status: 'healthy',
      replicas: '3/3',
      latency: 38,
      updatedAt: '10:42:18',
    },
    {
      name: 'image-detect-service',
      kind: 'Dubbo',
      status: 'healthy',
      replicas: '2/2',
      latency: 281,
      updatedAt: '10:42:16',
    },
    {
      name: 'audio-detect-service',
      kind: 'Dubbo',
      status: 'degraded',
      replicas: '1/2',
      latency: 1450,
      updatedAt: '10:42:11',
    },
    {
      name: 'video-detect-service',
      kind: 'Dubbo',
      status: 'healthy',
      replicas: '2/2',
      latency: 2130,
      updatedAt: '10:42:09',
    },
  ],
  recentReleases: releases,
}

const wait = () => new Promise((resolve) => setTimeout(resolve, 180))
const clone = <T>(value: T): T => structuredClone(value)

export const mockAdminApi: AdminApi = {
  async onlineTestProfile(contentType) {
    await wait()
    const saved = configs.find((item) => item.namespace === 'console.online-test' && item.configKey === contentType)
    return clone(saved ? JSON.parse(saved.configValue) as OnlineTestProfile
      : testProfiles[contentType] ?? { appId: '', sceneCode: '' })
  },
  async runOnlineTest(request) {
    await wait()
    const taskId = crypto.randomUUID()
    const asyncMode = request.executionMode === 'ASYNC'
    const result = asyncMode
      ? { taskId, status: 'ACCEPTED', resultExpected: true }
      : { taskId, status: 'SUCCEEDED', results: request.items.map((item) => ({ itemId: item.itemId, action: 'PASS' })) }
    testRecords.unshift({ id: Date.now(), contentType: request.contentType,
      executionMode: request.executionMode, appId: testProfiles[request.contentType]?.appId ?? '',
      sceneCode: testProfiles[request.contentType]?.sceneCode ?? '', itemCount: request.items.length,
      taskId, status: result.status, resultJson: JSON.stringify(result), durationMs: 180,
      createdAt: new Date().toISOString() })
    return { taskId, status: result.status, executionMode: request.executionMode,
      durationMs: 180, result }
  },
  async onlineTestRecords() {
    await wait()
    return clone(testRecords)
  },
  async login(request) {
    await wait()
    if (!request.tenantCode || !request.username || !request.password) {
      throw new Error('Tenant, username and password are required')
    }
    const session: AdminSession = {
      accessToken: 'mock-token',
      expiresAt: '2026-10-01T10:00:00',
      tenantId: identity.tenantId,
      tenantCode: request.tenantCode,
      userId: identity.userId,
      username: request.username,
      permissions,
    }
    return session
  },
  async logout() {
    await wait()
  },
  async me() {
    await wait()
    return clone(identity)
  },
  async dashboard() {
    await wait()
    return clone(dashboard)
  },
  async ruleSets() {
    await wait()
    return clone(ruleSets)
  },
  async rules(ruleSetCode) {
    await wait()
    return clone(rulesBySet[ruleSetCode] ?? [])
  },
  async validateRuleSet(ruleSetCode) {
    await wait()
    return {
      ruleSetCode,
      compiledRuleCount: rulesBySet[ruleSetCode]?.filter((item) => item.enabled).length ?? 0,
      valid: true,
    }
  },
  async publishRuleSet(ruleSetCode, version) {
    await wait()
    const target = ruleSets.find((item) => item.ruleSetCode === ruleSetCode)
    if (!target) throw new Error('Rule set not found')
    target.publishedVersion += 1
    target.version = version + 1
    const release: RuleRelease = {
      releaseId: Date.now(),
      tenantId: target.tenantId,
      ruleSetCode,
      releaseVersion: target.publishedVersion,
      scene: target.scene,
      snapshotJson: '{}',
      checksum: crypto.randomUUID().replaceAll('-', '').slice(0, 12),
      deleted: false,
      publishedBy: identity.username,
      publishedAt: new Date().toISOString().slice(0, 19),
    }
    releases.unshift(release)
    return clone(release)
  },
  async releases(ruleSetCode) {
    await wait()
    return clone(releases.filter((item) => item.ruleSetCode === ruleSetCode))
  },
  async configs(namespace) {
    await wait()
    return clone(configs.filter((item) => item.namespace === namespace))
  },
  async saveConfig(namespace, key, request) {
    await wait()
    const existing = configs.find((item) => item.namespace === namespace && item.configKey === key)
    if (existing) {
      existing.configValue = request.value
      existing.enabled = request.enabled
      existing.description = request.description
      existing.version += 1
      existing.updatedAt = new Date().toISOString().slice(0, 19)
      return clone(existing)
    }
    const created: ConfigItem = {
      id: Date.now(),
      tenantId: 1,
      namespace,
      configKey: key,
      configValue: request.value,
      version: 1,
      enabled: request.enabled,
      description: request.description,
      createdAt: new Date().toISOString().slice(0, 19),
      updatedAt: new Date().toISOString().slice(0, 19),
    }
    configs = [...configs, created]
    return clone(created)
  },
  async users() {
    await wait()
    return clone(users)
  },
  async roles() {
    await wait()
    return clone(roles)
  },
  async permissions() {
    await wait()
    return permissions.map((permissionCode) => ({
      permissionCode,
      permissionName: permissionCode,
    })) as PermissionItem[]
  },
  async auditLogs(filter) {
    await wait()
    return clone(
      auditLogs.filter((item) => {
        const keywordMatched =
          !filter.keyword ||
          item.taskId.includes(filter.keyword) ||
          item.traceId.includes(filter.keyword) ||
          item.appId.includes(filter.keyword)
        const actionMatched = !filter.action || item.action === filter.action
        const typeMatched = !filter.contentType || item.contentType === filter.contentType
        return keywordMatched && actionMatched && typeMatched
      }),
    )
  },
  async searchLogs(filter) {
    await wait()
    const items: LogEvent[] = auditLogs
      .filter((item) => !filter.taskId || item.taskId.includes(filter.taskId))
      .filter((item) => !filter.traceId || item.traceId.includes(filter.traceId))
      .filter((item) => !filter.appId || item.appId.includes(filter.appId))
      .map((item) => ({
        eventId: item.id,
        '@timestamp': new Date(Date.now() - Number(item.id) * 60_000).toISOString(),
        serviceName: 'study-text-detect',
        logSpace: 'content-risk',
        logType: 'CHAIN',
        level: item.degraded ? 'WARN' : 'INFO',
        traceId: item.traceId,
        taskId: item.taskId,
        itemId: 'item-001',
        appId: item.appId,
        sceneCode: item.sceneCode,
        source: 'text.detect.chain',
        eventType: 'CHAIN_END',
        message: item.action,
        success: !item.degraded,
        costMillis: item.durationMs,
      }))
      .filter((item) => item['@timestamp'] >= filter.from && item['@timestamp'] <= filter.to)
      .filter((item) => !filter.level || item.level === filter.level)
      .filter((item) => filter.success === undefined || item.success === filter.success)
      .filter((item) => !filter.serviceName || item.serviceName === filter.serviceName)
      .filter((item) => !filter.logSpace || item.logSpace === filter.logSpace)
    return { items, hasMore: false, nextCursor: '' }
  },
  async taskLogs(taskId) {
    await wait()
    const event = auditLogs.find((item) => item.taskId === taskId)
    return {
      taskId,
      truncated: false,
      events: event
        ? event.nodes.map((node, index) => ({
            eventId: `${event.id}-${index}`,
            '@timestamp': new Date(
              Date.now() - Number(event.id) * 60_000 - (event.nodes.length - index) * 1000,
            ).toISOString(),
            serviceName: 'study-text-detect',
            logSpace: 'content-risk',
            logType: 'CHAIN',
            level: node.status === 'error' ? 'ERROR' : 'INFO',
            traceId: event.traceId,
            taskId,
            itemId: 'item-001',
            appId: event.appId,
            sceneCode: event.sceneCode,
            source: 'text.detect.chain',
            eventType: 'EXECUTE_END',
            message: node.message,
            success: node.status !== 'error',
            costMillis: node.durationMs,
            handlerName: node.name,
          }))
        : [],
    }
  },
}
