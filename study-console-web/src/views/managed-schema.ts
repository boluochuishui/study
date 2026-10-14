import type { ManagedResource } from '@/types/admin'

export interface ManagedField {
  key: string
  kind?: 'text' | 'textarea' | 'number' | 'switch' | 'select' | 'reference' | 'json'
  required?: boolean
  options?: string[]
  reference?: ManagedResource
  immutable?: boolean
  default?: string | number | boolean
}

const enabled: ManagedField = { key: 'enabled', kind: 'switch', default: true }
const description: ManagedField = { key: 'description', kind: 'textarea' }
const contentType: ManagedField = {
  key: 'content_type',
  kind: 'select',
  required: true,
  immutable: true,
  options: ['TEXT', 'IMAGE', 'AUDIO', 'VIDEO'],
}

export const managedFields: Record<ManagedResource, ManagedField[]> = {
  apps: [
    { key: 'app_id', required: true, immutable: true },
    { key: 'app_name', required: true },
    { key: 'credential_ref', required: true },
    description,
    enabled,
  ],
  scenes: [
    { key: 'app_record_id', kind: 'reference', reference: 'apps', required: true, immutable: true },
    { key: 'scene_code', required: true, immutable: true },
    { key: 'scene_name', required: true },
    { key: 'default_language' },
    { key: 'default_region' },
    description,
    enabled,
  ],
  rules: [
    { key: 'scene_id', kind: 'reference', reference: 'scenes', required: true, immutable: true },
    { key: 'rule_code', required: true, immutable: true },
    { key: 'rule_name', required: true },
    contentType,
    {
      key: 'rule_type',
      kind: 'select',
      required: true,
      options: ['KEYWORD', 'REGEX', 'MEDIA_HASH', 'SPEL', 'LLM'],
    },
    {
      key: 'execution_mode',
      kind: 'select',
      required: true,
      default: 'BOTH',
      options: ['BOTH', 'SYNC', 'ASYNC'],
    },
    { key: 'priority', kind: 'number', default: 100 },
    { key: 'on_hit_action', kind: 'select', options: ['PASS', 'REVIEW', 'REJECT'] },
    { key: 'risk_labels_json', kind: 'json' },
    { key: 'parameters_json', kind: 'json' },
    { key: 'media_library_id', kind: 'reference', reference: 'libraries' },
    { key: 'spel_release_id', kind: 'number' },
    { key: 'spel_rule_id' },
    description,
    enabled,
  ],
  libraries: [
    { key: 'library_code', required: true, immutable: true },
    { key: 'library_name', required: true },
    contentType,
    {
      key: 'library_type',
      kind: 'select',
      required: true,
      options: ['BLOCKLIST', 'ALLOWLIST', 'TESTSET'],
    },
    description,
    enabled,
  ],
  assets: [
    { key: 'asset_name', required: true },
    contentType,
    { key: 'content_sha256', required: true, immutable: true },
    { key: 'hash_version', required: true, immutable: true },
    {
      key: 'storage_status',
      kind: 'select',
      required: true,
      default: 'READY',
      options: ['PENDING', 'READY', 'FAILED'],
    },
    description,
    enabled,
  ],
  memberships: [
    {
      key: 'library_id',
      kind: 'reference',
      reference: 'libraries',
      required: true,
      immutable: true,
    },
    { key: 'asset_id', kind: 'reference', reference: 'assets', required: true, immutable: true },
    { key: 'labels_json', kind: 'json' },
    description,
    enabled,
  ],
}

export const resourceGroups: Record<'access' | 'media', ManagedResource[]> = {
  access: ['apps', 'scenes', 'rules'],
  media: ['libraries', 'assets', 'memberships'],
}

export function primaryKey(resource: ManagedResource): string {
  return {
    apps: 'app_id',
    scenes: 'scene_code',
    rules: 'rule_code',
    libraries: 'library_code',
    assets: 'asset_name',
    memberships: 'asset_id',
  }[resource]
}
