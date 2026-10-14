export type PermissionCode =
  | 'CONFIG_READ'
  | 'CONFIG_WRITE'
  | 'RULE_READ'
  | 'RULE_WRITE'
  | 'RULE_PUBLISH'
  | 'IAM_READ'
  | 'IAM_WRITE'
  | 'LOG_READ'
  | 'TEST_READ'
  | 'TEST_RUN'

export type DetectContentType = 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO'

export type ManagedResource = 'apps' | 'scenes' | 'rules' | 'libraries' | 'assets' | 'memberships'

export type ManagedRecord = Record<string, string | number | boolean | null>

export interface SaveManagedRecordRequest {
  version: number
  values: Record<string, string | number | boolean | null>
}

export interface OnlineTestProfile {
  appId: string
  sceneCode: string
}

export interface OnlineTestItem {
  itemId: string
  text?: string
  url?: string
  data?: string
}

export interface OnlineTestRequest {
  contentType: DetectContentType
  executionMode: 'SYNC' | 'ASYNC'
  language: string
  region: string
  items: OnlineTestItem[]
}

export interface OnlineTestResult {
  taskId: string | null
  status: string
  executionMode: string
  durationMs: number
  result: Record<string, unknown>
}

export interface OnlineTestRecord {
  id: number
  contentType: DetectContentType
  executionMode: string
  appId: string
  sceneCode: string
  itemCount: number
  taskId: string | null
  status: string
  resultJson: string | null
  durationMs: number
  createdAt: string
}

export interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  timestamp: string
}

export interface LoginRequest {
  tenantCode: string
  username: string
  password: string
}

export interface AdminSession {
  accessToken: string
  expiresAt: string
  tenantId: number
  tenantCode: string
  userId: number
  username: string
  permissions: PermissionCode[]
}

export interface AdminIdentity {
  tenantId: number
  tenantCode: string
  userId: number
  username: string
  displayName: string
  permissions: PermissionCode[]
}

export interface MetricItem {
  key: string
  value: string
  change: string
  trend: 'up' | 'down' | 'steady'
}

export interface ServiceHealth {
  name: string
  kind: string
  status: 'healthy' | 'degraded' | 'offline'
  replicas: string
  latency: number
  updatedAt: string
}

export interface DashboardSnapshot {
  metrics: MetricItem[]
  services: ServiceHealth[]
  recentReleases: RuleRelease[]
}

export interface RuleSet {
  id: number
  tenantId: number
  ruleSetCode: string
  ruleSetName: string
  scene: string
  evaluationMode: string
  errorPolicy: string
  publishedVersion: number
  version: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface RuleDraft {
  id: number
  tenantId: number
  ruleSetId: number
  ruleId: string
  ruleName: string
  expression: string
  priority: number
  attributesJson: string
  version: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface RuleRelease {
  releaseId: number
  tenantId: number
  ruleSetCode: string
  releaseVersion: number
  scene: string
  snapshotJson: string
  checksum: string
  deleted: boolean
  publishedBy: string
  publishedAt: string
}

export interface RuleValidationResult {
  ruleSetCode: string
  compiledRuleCount: number
  valid: boolean
}

export interface ConfigItem {
  id: number
  tenantId: number
  namespace: string
  configKey: string
  configValue: string
  version: number
  enabled: boolean
  description: string
  createdAt: string
  updatedAt: string
}

export interface SaveConfigRequest {
  value: string
  enabled: boolean
  description: string
  version?: number
}

export interface AdminUser {
  id: number
  tenantId: number
  username: string
  displayName: string
  enabled: boolean
  roleCodes?: string[]
}

export interface AdminRole {
  id: number
  tenantId: number
  roleCode: string
  roleName: string
  enabled: boolean
  permissions: PermissionCode[]
}

export interface PermissionItem {
  permissionCode: PermissionCode
  permissionName: string
}

export interface AuditLog {
  id: string
  occurredAt: string
  taskId: string
  traceId: string
  appId: string
  sceneCode: string
  contentType: 'TEXT' | 'IMAGE' | 'AUDIO' | 'VIDEO'
  action: 'PASS' | 'REVIEW' | 'REJECT' | 'ERROR'
  durationMs: number
  degraded: boolean
  labels: string[]
  nodes: AuditNode[]
}

export interface AuditNode {
  name: string
  status: 'success' | 'warning' | 'error'
  durationMs: number
  message: string
}

export interface AuditFilter {
  keyword?: string
  action?: string
  contentType?: string
}

export interface LogEvent {
  eventId: string
  '@timestamp': string
  serviceName: string
  logSpace: string
  logType: string
  level: string
  traceId: string
  taskId: string
  itemId?: string
  appId: string
  sceneCode: string
  source: string
  eventType: string
  message: string
  success: boolean
  costMillis: number
  handlerName?: string
}

export interface LogFilter {
  from: string
  to: string
  taskId?: string
  traceId?: string
  appId?: string
  serviceName?: string
  logSpace?: string
  level?: string
  success?: boolean
  cursor?: string
  size?: number
}

export interface LogPage {
  items: LogEvent[]
  hasMore: boolean
  nextCursor: string
}

export interface TaskLogView {
  taskId: string
  events: LogEvent[]
  truncated: boolean
}
