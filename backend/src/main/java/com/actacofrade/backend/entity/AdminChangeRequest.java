package com.actacofrade.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Request sent by a hermandad member asking the super admin to replace
 * the current administrator. Starts as PENDING; only the super admin can
 * move it to APPROVED or REJECTED.
 */
@Entity
@Table(name = "admin_change_requests", indexes = {
        @Index(name = "idx_admin_change_requests_status", columnList = "status"),
        @Index(name = "idx_admin_change_requests_hermandad", columnList = "hermandad_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Hermandad targeted by the request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hermandad_id", nullable = false)
    private Hermandad hermandad;

    /** Member who sent the request. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    /** Reason written by the requester (sanitized before saving). */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /** Current status of the request. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private AdminChangeRequestStatus status = AdminChangeRequestStatus.PENDING;

    /** New administrator chosen on approval (null otherwise). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_admin_user_id")
    private User newAdmin;

    /** Super admin who resolved the request (null while pending). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_id")
    private User resolvedBy;

    /** When the request was resolved. */
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    /** Creation timestamp (immutable). */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
