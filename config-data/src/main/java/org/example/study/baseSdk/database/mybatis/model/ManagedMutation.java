package org.example.study.baseSdk.database.mybatis.model;

import java.util.List;

/** 已经过白名单校验的 SQL 写入参数。 */
public class ManagedMutation {
    private Long id;
    private final String table;
    private final long tenantId;
    private final long actorId;
    private final long version;
    private final List<FieldValue> fields;

    public ManagedMutation(Long id, String table, long tenantId, long actorId, long version,
                           List<FieldValue> fields) {
        this.id = id;
        this.table = table;
        this.tenantId = tenantId;
        this.actorId = actorId;
        this.version = version;
        this.fields = List.copyOf(fields);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTable() { return table; }
    public long getTenantId() { return tenantId; }
    public long getActorId() { return actorId; }
    public long getVersion() { return version; }
    public List<FieldValue> getFields() { return fields; }

    public static final class FieldValue {
        private final String column;
        private final Object value;

        public FieldValue(String column, Object value) {
            this.column = column;
            this.value = value;
        }

        public String getColumn() { return column; }
        public Object getValue() { return value; }
    }
}
