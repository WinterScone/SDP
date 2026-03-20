package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "activity_log", indexes = {
    @Index(name = "idx_activity_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_activity_log_admin", columnList = "admin_id")
})
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String activityType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_username")
    private String adminUsername;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "medicine_name")
    private String medicineName;

    @Column(name = "additional_details", columnDefinition = "TEXT")
    private String additionalDetails;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
