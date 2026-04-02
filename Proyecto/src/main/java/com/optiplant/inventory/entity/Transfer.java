package com.optiplant.inventory.entity;

import com.optiplant.inventory.enumtype.TransferPriority;
import com.optiplant.inventory.enumtype.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transfer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requesting_branch_id", nullable = false)
    private Branch requestingBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_branch_id", nullable = false)
    private Branch sourceBranch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_branch_id", nullable = false)
    private Branch destinationBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status = TransferStatus.REQUESTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferPriority priority = TransferPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "transport_company", length = 120)
    private String transportCompany;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "estimated_arrival_date")
    private LocalDateTime estimatedArrivalDate;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;
}