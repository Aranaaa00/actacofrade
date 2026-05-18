package com.actacofrade.backend.entity;

/**
 * Discriminator for a support request created from the user-facing support modal.
 * ADMIN_CHANGE keeps the legacy admin-replacement flow; the rest are tracking-only
 * requests resolved manually by the super administrator.
 */
public enum SupportRequestType {
    ADMIN_CHANGE,
    PASSWORD_RESET,
    VERIFICATION,
    CONTACT
}
