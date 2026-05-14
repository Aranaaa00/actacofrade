package com.actacofrade.backend.service;

public final class SuperAdminAuditActions {

    public static final String ENTITY_TYPE_USER = "USER";

    public static final String ACTION_STATUS_CHANGE = "SUPERADMIN_STATUS_CHANGE";
    public static final String ACTION_MANUAL_VERIFY = "SUPERADMIN_MANUAL_VERIFY";
    public static final String ACTION_MANUAL_UNVERIFY = "SUPERADMIN_MANUAL_UNVERIFY";
    public static final String ACTION_ROLE_OVERRIDE = "SUPERADMIN_ROLE_OVERRIDE";
    public static final String ACTION_PASSWORD_RESET_TRIGGERED = "SUPERADMIN_PASSWORD_RESET";

    private SuperAdminAuditActions() {
    }
}
