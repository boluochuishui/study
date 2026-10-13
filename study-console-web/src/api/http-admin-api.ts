import type { AdminApi } from './contracts'
import { httpClient, unwrap } from './client'
import type {
  AdminIdentity,
  AdminRole,
  AdminSession,
  AdminUser,
  ApiEnvelope,
  AuditFilter,
  AuditLog,
  LogFilter,
  LogPage,
  TaskLogView,
  ConfigItem,
  DashboardSnapshot,
  LoginRequest,
  PermissionItem,
  RuleDraft,
  RuleRelease,
  RuleSet,
  RuleValidationResult,
  SaveConfigRequest,
  DetectContentType,
  OnlineTestProfile,
  OnlineTestRequest,
  OnlineTestResult,
  OnlineTestRecord,
} from '@/types/admin'

const EMPTY_DASHBOARD: DashboardSnapshot = {
  metrics: [],
  services: [],
  recentReleases: [],
}

export const httpAdminApi: AdminApi = {
  async login(request: LoginRequest) {
    return unwrap(
      (await httpClient.post<ApiEnvelope<AdminSession>>('/admin/auth/login', request)).data,
    )
  },
  async logout() {
    await httpClient.post('/admin/auth/logout')
  },
  async me() {
    return unwrap((await httpClient.get<ApiEnvelope<AdminIdentity>>('/admin/auth/me')).data)
  },
  async dashboard() {
    return EMPTY_DASHBOARD
  },
  async ruleSets() {
    return unwrap((await httpClient.get<ApiEnvelope<RuleSet[]>>('/admin/rule-sets')).data)
  },
  async rules(ruleSetCode: string) {
    return unwrap(
      (await httpClient.get<ApiEnvelope<RuleDraft[]>>(`/admin/rule-sets/${ruleSetCode}/rules`))
        .data,
    )
  },
  async validateRuleSet(ruleSetCode: string) {
    return unwrap(
      (
        await httpClient.post<ApiEnvelope<RuleValidationResult>>(
          `/admin/rule-sets/${ruleSetCode}/validate`,
        )
      ).data,
    )
  },
  async publishRuleSet(ruleSetCode: string, version: number) {
    return unwrap(
      (
        await httpClient.post<ApiEnvelope<RuleRelease>>(`/admin/rule-sets/${ruleSetCode}/publish`, {
          version,
          publishedBy: 'console',
        })
      ).data,
    )
  },
  async releases(ruleSetCode: string) {
    return unwrap(
      (await httpClient.get<ApiEnvelope<RuleRelease[]>>(`/admin/rule-sets/${ruleSetCode}/releases`))
        .data,
    )
  },
  async configs(namespace: string) {
    return unwrap(
      (
        await httpClient.get<ApiEnvelope<ConfigItem[]>>('/admin/configs', {
          params: { namespace },
        })
      ).data,
    )
  },
  async saveConfig(namespace: string, key: string, request: SaveConfigRequest) {
    return unwrap(
      (await httpClient.put<ApiEnvelope<ConfigItem>>(`/admin/configs/${namespace}/${key}`, request))
        .data,
    )
  },
  async users() {
    return unwrap((await httpClient.get<ApiEnvelope<AdminUser[]>>('/admin/iam/users')).data)
  },
  async roles() {
    return unwrap((await httpClient.get<ApiEnvelope<AdminRole[]>>('/admin/iam/roles')).data)
  },
  async permissions() {
    return unwrap(
      (await httpClient.get<ApiEnvelope<PermissionItem[]>>('/admin/iam/permissions')).data,
    )
  },
  async auditLogs(filter: AuditFilter): Promise<AuditLog[]> {
    void filter
    return []
  },
  async searchLogs(filter: LogFilter) {
    return unwrap(
      (await httpClient.get<ApiEnvelope<LogPage>>('/admin/logs', { params: filter })).data,
    )
  },
  async taskLogs(taskId: string, from: string, to: string) {
    return unwrap(
      (
        await httpClient.get<ApiEnvelope<TaskLogView>>(
          `/admin/logs/tasks/${encodeURIComponent(taskId)}`,
          { params: { from, to } },
        )
      ).data,
    )
  },
  async onlineTestProfile(contentType: DetectContentType) {
    return unwrap((await httpClient.get<ApiEnvelope<OnlineTestProfile>>(
      `/admin/online-tests/profiles/${contentType}`)).data)
  },
  async runOnlineTest(request: OnlineTestRequest) {
    return unwrap((await httpClient.post<ApiEnvelope<OnlineTestResult>>(
      '/admin/online-tests', request, { timeout: 45_000 })).data)
  },
  async onlineTestRecords() {
    return unwrap((await httpClient.get<ApiEnvelope<OnlineTestRecord[]>>('/admin/online-tests')).data)
  },
}
