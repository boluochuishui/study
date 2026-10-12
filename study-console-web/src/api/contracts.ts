import type {
  AdminIdentity,
  AdminRole,
  AdminSession,
  AdminUser,
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

export interface AdminApi {
  login(request: LoginRequest): Promise<AdminSession>
  logout(): Promise<void>
  me(): Promise<AdminIdentity>
  dashboard(): Promise<DashboardSnapshot>
  ruleSets(): Promise<RuleSet[]>
  rules(ruleSetCode: string): Promise<RuleDraft[]>
  validateRuleSet(ruleSetCode: string): Promise<RuleValidationResult>
  publishRuleSet(ruleSetCode: string, version: number): Promise<RuleRelease>
  releases(ruleSetCode: string): Promise<RuleRelease[]>
  configs(namespace: string): Promise<ConfigItem[]>
  saveConfig(namespace: string, key: string, request: SaveConfigRequest): Promise<ConfigItem>
  users(): Promise<AdminUser[]>
  roles(): Promise<AdminRole[]>
  permissions(): Promise<PermissionItem[]>
  auditLogs(filter: AuditFilter): Promise<AuditLog[]>
}
