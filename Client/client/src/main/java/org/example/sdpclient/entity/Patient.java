package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    private String email;
    private String phone;

    @Column(nullable = false)
    private boolean smsConsent = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "linked_admin_id")
    private Long linkedAdminId;

    @Column(name = "linked_admin_name")
    private String linkedAdminName;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "face_data")
    private byte[] faceData;

    @Column(name = "face_content_type")
    private String faceContentType;

    @Column(name = "face_enrolled_at")
    private LocalDateTime faceEnrolledAt;

    @Column(name = "face_active", nullable = false)
    private boolean faceActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}