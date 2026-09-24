package org.example.study.baseSdk.database.mybatis.entity;

/** 管理台权限实体。 */
public class AdminPermissionEntity {

    private String permissionCode;
    private String permissionName;

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
}
