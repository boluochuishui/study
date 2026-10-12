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
  ConfigItem,
  DashboardSnapshot,
  LoginRequest,
  PermissionItem,
  RuleDraft,
  RuleRelease,
  RuleSet,
  RuleValidationResult,
  SaveConfigRequest,
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
}
