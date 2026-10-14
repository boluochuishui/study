import type {
  AdminIdentity,
  AdminRole,
  AdminSession,
  AdminUser,
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
  ManagedResource,
  ManagedRecord,
  SaveManagedRecordRequest,
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
  searchLogs(filter: LogFilter): Promise<LogPage>
  taskLogs(taskId: string, from: string, to: string): Promise<TaskLogView>
  onlineTestProfile(contentType: DetectContentType): Promise<OnlineTestProfile>
  runOnlineTest(request: OnlineTestRequest): Promise<OnlineTestResult>
  onlineTestRecords(): Promise<OnlineTestRecord[]>
  managedRecords(resource: ManagedResource): Promise<ManagedRecord[]>
  createManagedRecord(
    resource: ManagedResource,
    request: SaveManagedRecordRequest,
  ): Promise<ManagedRecord>
  updateManagedRecord(
    resource: ManagedResource,
    id: number,
    request: SaveManagedRecordRequest,
  ): Promise<ManagedRecord>
}
