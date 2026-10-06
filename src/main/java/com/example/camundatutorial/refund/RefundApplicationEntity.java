package com.example.camundatutorial.refund;

import com.example.camundatutorial.governance.Sensitive;
import com.example.camundatutorial.governance.SensitiveType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import org.hibernate.annotations.Comment;

/**
 * Business record for a refund application, persisted alongside Camunda process state.
 */
@Data
@Entity
@Table(name = "refund_application")
@Comment("Refund application business record tracked through the Camunda refund process lifecycle")
public class RefundApplicationEntity {

    /** Internal DB key (rarely used in the process). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("Internal database primary key; auto-generated and rarely used by the process")
    private Long id;

    /** Business id (REF-…) used by the process and API. */
    @Column(nullable = false, unique = true)
    @Comment("Business application identifier (e.g. REF-XXXXXXXX) used by the process and API")
    private String applicationId;

    /** Link to Camunda’s running/historic process. */
    @Comment("Camunda process instance id linking this row to engine runtime and history tables")
    private String processInstanceId;

    /** Applicant first name from the REST request. */
    @Sensitive(SensitiveType.PII)
    @Comment("Applicant first name submitted via the refund application REST API")
    private String firstName;

    /** Applicant last name from the REST request. */
    @Sensitive(SensitiveType.PII)
    @Comment("Applicant last name submitted via the refund application REST API")
    private String lastName;

    /** Document reference number set by the DRN/station service task. */
    @Comment("Document reference number (DRN) assigned when the application is routed to a station")
    private String drn;

    /** Tax station/office set by the DRN/station service task. */
    @Comment("Tax station or office assigned to handle the refund application")
    private String station;

    /** Risk classification: low / medium / high. */
    @Comment("Taxpayer risk level classification: low, medium, or high")
    private String riskLevel;

    /** Decision flag (auto-approve or from user task). */
    @Comment("Whether the refund was approved (true) or rejected (false); set by auto-approve or manual review")
    private Boolean approved;

    /** Lifecycle snapshot (CREATED, ASSIGNED, PENDING_REVIEW, …). */
    @Comment("Lifecycle status snapshot such as CREATED, ASSIGNED, PENDING_REVIEW, AUTO_APPROVED, APPROVED, REJECTED")
    private String status;

    /** Auto-generated or reviewer comment. */
    @Sensitive(SensitiveType.PII)
    @Comment("Review comment from automatic approval or from the manual reviewer user task")
    private String reviewComment;

    /** Message sent to the taxpayer. */
    @Sensitive(SensitiveType.PII)
    @Column(length = 1000)
    @Comment("Notification message content sent to the taxpayer after the refund decision")
    private String notificationMessage;

    /** Final outcome: APPROVED / REJECTED. */
    @Comment("Final decision outcome: APPROVED or REJECTED")
    private String decision;

    /** When the row was created. */
    @Comment("Timestamp when the refund application row was first created")
    private Instant createdAt;

    /** When the row was last changed. */
    @Comment("Timestamp when the refund application row was last updated")
    private Instant updatedAt;
}
