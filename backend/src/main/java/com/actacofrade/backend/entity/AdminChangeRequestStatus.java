package com.actacofrade.backend.entity;

/**
 * Status of an admin change request for a hermandad.
 *
 * <ul>
 *   <li>{@link #PENDING}: waiting for the super admin review.</li>
 *   <li>{@link #APPROVED}: approved and applied (a new user is now administrator).</li>
 *   <li>{@link #REJECTED}: rejected by the super admin; no changes are applied.</li>
 * </ul>
 */
public enum AdminChangeRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
