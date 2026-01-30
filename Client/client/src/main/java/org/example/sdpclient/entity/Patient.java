package org.example.sdpclient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
    private String dateOfBirth;

    private String email;
    private String phone;

    @Column(name="created_at")
    private String createdAt;

    @Column(name = "linked_admin_id")
    private Long linkedAdminId;

    @Column(name = "linked_admin_name")
    private String linkedAdminName;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now().toString();
    }
}
